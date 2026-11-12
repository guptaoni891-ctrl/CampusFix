package com.campusfix;

import com.campusfix.model.Administrator;
import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.Student;
import com.campusfix.model.Technician;
import com.campusfix.model.TicketEventType;
import com.campusfix.model.User;
import com.campusfix.persistence.FileTicketEventRepository;
import com.campusfix.persistence.FileTicketRepository;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.KeywordSpecializationMatcher;
import com.campusfix.service.PriorityAssessment;
import com.campusfix.service.SlaAssessment;
import com.campusfix.service.SlaService;
import com.campusfix.service.SlaStatus;
import com.campusfix.service.StandardSlaPolicy;
import com.campusfix.service.TechnicianRecommendation;
import com.campusfix.service.TechnicianRecommendationService;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.ValidationException;
import com.campusfix.service.WeightedPriorityScorer;
import com.campusfix.util.CsvExporter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/** Deterministic coverage for priority, SLA, recommendation, and reassignment behavior. */
public final class OperationalIntelligenceTest {
    private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final LocalDateTime LOCAL_NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    private OperationalIntelligenceTest() {
    }

    public static void run() throws Exception {
        testPriorityWeightsAndBoundaries();
        testSlaCalculations();
        testRecommendationRanking();
        testInProgressReassignmentAndAuthorization();
        System.out.println("PASS: explainable priority, SLA deadlines and compliance, technician recommendations, safe IN_PROGRESS reassignment");
    }

    private static void testPriorityWeightsAndBoundaries() {
        WeightedPriorityScorer scorer = new WeightedPriorityScorer();
        Complaint agedCritical = complaint(1, Category.ELECTRICAL, Severity.CRITICAL,
            LOCAL_NOW.minusDays(10), Status.OPEN, null);
        PriorityAssessment maximum = scorer.assess(agedCritical, LOCAL_NOW);
        require(maximum.score() == 100, "Priority must cap at 100");
        require(maximum.severityPoints() == 55 && maximum.categoryPoints() == 15
            && maximum.agePoints() == 20 && maximum.operationalPoints() == 10,
            "Priority breakdown must use documented weights");

        Complaint futureLow = complaint(2, Category.OTHER, Severity.LOW,
            LOCAL_NOW.plusHours(5), Status.OPEN, null);
        PriorityAssessment nonNegative = scorer.assess(futureLow, LOCAL_NOW);
        require(nonNegative.score() >= 0 && nonNegative.agePoints() == 0,
            "Future timestamps must not create negative age points");

        Complaint twelveHoursOld = complaint(3, Category.OTHER, Severity.LOW,
            LOCAL_NOW.minusHours(12), Status.OPEN, null);
        require(scorer.assess(twelveHoursOld, LOCAL_NOW).agePoints() == 2,
            "Age must add one point per six complete hours");
        require(maximum.explanation().contains("Severity 55") && maximum.explanation().endsWith("100/100."),
            "Priority must explain every weighted component");
    }

    private static void testSlaCalculations() {
        StandardSlaPolicy policy = new StandardSlaPolicy();
        SlaService service = new SlaService(policy, CLOCK);
        require(policy.targetFor(Severity.CRITICAL).toHours() == 4, "Critical SLA target");
        require(policy.targetFor(Severity.HIGH).toHours() == 24, "High SLA target");
        require(policy.targetFor(Severity.MEDIUM).toHours() == 72, "Medium SLA target");
        require(policy.targetFor(Severity.LOW).toHours() == 168, "Low SLA target");

        Complaint onTrack = complaint(10, Category.ELECTRICAL, Severity.CRITICAL,
            LOCAL_NOW.minusHours(2), Status.OPEN, null);
        SlaAssessment onTrackSla = service.assess(onTrack, null);
        require(onTrackSla.status() == SlaStatus.ON_TRACK && onTrackSla.timeRemaining().toHours() == 2,
            "Critical ticket with two hours remaining must be on track");

        Complaint dueSoon = complaint(11, Category.ELECTRICAL, Severity.CRITICAL,
            LOCAL_NOW.minusMinutes(210), Status.ASSIGNED, "T01");
        require(service.assess(dueSoon, null).status() == SlaStatus.DUE_SOON,
            "Critical ticket inside its one-hour warning window must be due soon");

        Complaint overdue = complaint(12, Category.INTERNET, Severity.HIGH,
            LOCAL_NOW.minusHours(25), Status.IN_PROGRESS, "T01");
        require(service.assess(overdue, null).status() == SlaStatus.OVERDUE,
            "Unresolved ticket past its deadline must be overdue");

        Complaint resolved = complaint(13, Category.OTHER, Severity.LOW,
            LOCAL_NOW.minusHours(170), Status.RESOLVED, "T02");
        require(service.assess(resolved, LOCAL_NOW.minusHours(1)).status() == SlaStatus.RESOLVED_BREACHED,
            "Reliable late resolution timestamp must report SLA breach");
        require(service.assess(resolved, LOCAL_NOW.minusHours(3)).status() == SlaStatus.RESOLVED_MET,
            "Reliable early resolution timestamp must report SLA compliance");
        require(service.assess(resolved, null).status() == SlaStatus.RESOLVED_UNKNOWN,
            "Legacy resolved ticket without a resolution event must not fabricate compliance");
    }

    private static void testRecommendationRanking() throws Exception {
        Path directory = Files.createTempDirectory("campusfix-recommendation-test-");
        try {
            FileTicketRepository repository = new FileTicketRepository(directory.resolve("complaints.tsv"));
            Technician busySpecialist = new Technician("T10", "Busy Electrician", "busy", "Electrical");
            Technician availableSpecialist = new Technician("T11", "Available Electrician", "available", "Electrical / Power");
            Technician unmatched = new Technician("T12", "Plumber", "plumber", "Plumbing");
            UserDirectory directoryUsers = new UserDirectory(List.of(
                new Administrator("A10", "Admin", "admin-test"),
                new Student("S10", "Student", "student-test"),
                busySpecialist, availableSpecialist, unmatched));
            repository.save(complaint(20, Category.ELECTRICAL, Severity.HIGH,
                LOCAL_NOW.minusHours(1), Status.ASSIGNED, busySpecialist.getId()));
            repository.save(complaint(21, Category.INTERNET, Severity.MEDIUM,
                LOCAL_NOW.minusHours(2), Status.IN_PROGRESS, busySpecialist.getId()));
            Complaint target = complaint(22, Category.ELECTRICAL, Severity.HIGH,
                LOCAL_NOW, Status.OPEN, null);
            repository.save(target);

            TechnicianRecommendationService recommendationService = new TechnicianRecommendationService(
                repository, directoryUsers, new KeywordSpecializationMatcher());
            List<TechnicianRecommendation> recommendations = recommendationService.recommendationsFor(target);
            require(recommendations.get(0).technician().getId().equals(availableSpecialist.getId()),
                "Equal specialty matches must favor the lower active workload");
            require(recommendations.get(0).activeWorkload() == 0,
                "Technicians with no assigned complaints must report zero workload");
            require(recommendations.get(0).explanation().contains("matches")
                && recommendations.get(0).explanation().contains("Active workload: 0"),
                "Recommendation must explain specialty and workload");

            Complaint noMatch = complaint(23, Category.CLEANING, Severity.LOW,
                LOCAL_NOW, Status.OPEN, null);
            List<TechnicianRecommendation> fallbacks = recommendationService.recommendationsFor(noMatch);
            require(fallbacks.stream().noneMatch(TechnicianRecommendation::isSpecialtyMatch),
                "No suitable specialty must be represented honestly");
            require(fallbacks.get(0).explanation().contains("fallback"),
                "No-match recommendation must explain fallback ranking");

            UserDirectory noTechnicians = new UserDirectory(List.of(
                new Administrator("A11", "Admin", "admin-empty")));
            require(new TechnicianRecommendationService(repository, noTechnicians,
                new KeywordSpecializationMatcher()).recommendationsFor(target).isEmpty(),
                "No-technician directory must return an empty recommendation list");
        } finally {
            deleteTree(directory);
        }
    }

    private static void testInProgressReassignmentAndAuthorization() throws Exception {
        Path directory = Files.createTempDirectory("campusfix-reassignment-test-");
        try {
            FileTicketRepository tickets = new FileTicketRepository(directory.resolve("complaints.tsv"));
            FileTicketEventRepository events = new FileTicketEventRepository(directory.resolve("events.tsv"));
            UserDirectory users = new UserDirectory();
            ComplaintService service = new ComplaintService(
                tickets, events, users, new WeightedPriorityScorer(), CLOCK);
            User student = users.login("student", "demo123");
            User administrator = users.login("admin", "demo123");
            User firstTechnician = users.login("tech", "demo123");
            User secondTechnician = users.login("tech2", "demo123");
            Complaint ticket = service.create(student, "Electrical panel vibration",
                "The electrical panel is vibrating and making a loud buzzing sound.",
                "Engineering B12", Category.ELECTRICAL, Severity.HIGH);

            service.assign(administrator, ticket.getId(), users.technicians().get(0));
            service.changeStatus(firstTechnician, ticket.getId(), Status.IN_PROGRESS);
            expectValidation(() -> service.recommendTechnicians(student, ticket.getId()));
            service.assign(administrator, ticket.getId(), users.technicians().get(1));

            Complaint reassigned = service.get(administrator, ticket.getId());
            require(reassigned.getStatus() == Status.IN_PROGRESS,
                "Reassigning an IN_PROGRESS complaint must preserve IN_PROGRESS");
            require(reassigned.getTechnicianId().equals(secondTechnician.getId()),
                "Reassignment must store the new technician");
            require(service.eventsFor(administrator, ticket.getId()).stream()
                .anyMatch(event -> event.getEventType() == TicketEventType.TECHNICIAN_REASSIGNED
                    && event.getNote().equals(firstTechnician.getId() + "|" + secondTechnician.getId())),
                "Reassignment event must identify both previous and new technicians");

            expectValidation(() -> service.addProgressNote(firstTechnician, ticket.getId(),
                "The old technician must no longer have access."));
            expectValidation(() -> service.changeStatus(firstTechnician, ticket.getId(), Status.RESOLVED,
                "The old technician must not resolve the reassigned ticket."));
            service.addProgressNote(secondTechnician, ticket.getId(),
                "Accepted reassigned work and continued the electrical inspection.");
            service.changeStatus(secondTechnician, ticket.getId(), Status.RESOLVED,
                "Secured the panel mounting and verified the vibration has stopped.");
            require(service.get(administrator, ticket.getId()).getStatus() == Status.RESOLVED,
                "New technician must retain normal workflow access");

            Path csv = directory.resolve("metrics.csv");
            CsvExporter.export(csv, service.all(), service);
            String exported = Files.readString(csv);
            require(exported.contains("Priority Explanation") && exported.contains("SLA Deadline")
                && exported.contains(String.valueOf(service.score(service.all().get(0)))),
                "CSV must use service priority and SLA calculations");
        } finally {
            deleteTree(directory);
        }
    }

    private static Complaint complaint(int id, Category category, Severity severity,
                                       LocalDateTime createdAt, Status status, String technicianId) {
        return new Complaint(id, "S01", "Test complaint " + id,
            "A deterministic complaint used by the automated test suite.", "Test Lab",
            category, severity, createdAt, status, technicianId, createdAt);
    }

    private static void expectValidation(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected validation failure");
        } catch (ValidationException expected) {
            // Expected.
        }
    }

    private static void deleteTree(Path directory) throws Exception {
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (Exception ignored) { }
            });
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
