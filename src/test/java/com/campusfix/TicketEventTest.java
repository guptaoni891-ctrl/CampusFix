package com.campusfix;

import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.TicketEvent;
import com.campusfix.model.TicketEventType;
import com.campusfix.model.User;
import com.campusfix.persistence.FileTicketEventRepository;
import com.campusfix.persistence.FileTicketRepository;
import com.campusfix.persistence.SqliteTicketEventRepository;
import com.campusfix.persistence.SqliteTicketRepository;
import com.campusfix.persistence.TicketEventRepository;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.ValidationException;
import com.campusfix.service.WeightedPriorityScorer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/** Focused deterministic coverage for ticket activity, authorization, and event persistence. */
public final class TicketEventTest {
    private TicketEventTest() {
    }

    public static void run() throws Exception {
        Path directory = Files.createTempDirectory("campusfix-events-test-");
        try {
            Clock clock = Clock.fixed(Instant.parse("2026-11-20T08:30:00Z"), ZoneOffset.UTC);
            Path complaintFile = directory.resolve("complaints.tsv");
            Path eventFile = directory.resolve("events.tsv");
            FileTicketRepository tickets = new FileTicketRepository(complaintFile);
            FileTicketEventRepository events = new FileTicketEventRepository(eventFile);
            UserDirectory users = new UserDirectory();
            ComplaintService service = new ComplaintService(
                tickets, events, users, new WeightedPriorityScorer(), clock);
            User student = users.login("student", "demo123");
            User otherStudent = users.login("student2", "demo123");
            User administrator = users.login("admin", "demo123");
            User technician = users.login("tech", "demo123");
            User otherTechnician = users.login("tech2", "demo123");

            Complaint complaint = service.create(student, "Network cabinet overheating",
                "The network cabinet fan is making noise and the enclosure is very hot.",
                "Computer Lab 204", Category.INTERNET, Severity.HIGH);
            List<TicketEvent> created = service.eventsFor(student, complaint.getId());
            require(created.size() == 1, "Creation must produce exactly one activity event");
            require(created.get(0).getEventType() == TicketEventType.TICKET_CREATED,
                "Creation event type");
            require(created.get(0).getTimestamp().equals(LocalDateTime.of(2026, 11, 20, 8, 30)),
                "Injected clock must control event timestamps");

            expectValidation(() -> service.eventsFor(otherStudent, complaint.getId()));
            expectValidation(() -> service.addComment(otherStudent, complaint.getId(), "Not my ticket"));
            expectValidation(() -> service.addComment(student, complaint.getId(), " "));
            service.addComment(student, complaint.getId(), "The cabinet alarm has started sounding.");
            service.addComment(administrator, complaint.getId(), "Facilities team has been notified.");

            service.assign(administrator, complaint.getId(), users.technicians().get(0));
            expectValidation(() -> service.addProgressNote(otherTechnician, complaint.getId(),
                "Investigating the cabinet."));
            service.addProgressNote(technician, complaint.getId(),
                "Inspected the cabinet and isolated the failed cooling fan.");
            expectValidation(() -> service.changeStatus(student, complaint.getId(), Status.IN_PROGRESS));
            service.changeStatus(technician, complaint.getId(), Status.IN_PROGRESS);
            expectValidation(() -> service.changeStatus(technician, complaint.getId(), Status.RESOLVED));
            expectValidation(() -> service.changeStatus(technician, complaint.getId(), Status.RESOLVED, "short"));
            service.changeStatus(technician, complaint.getId(), Status.RESOLVED,
                "Replaced the cooling fan and verified normal cabinet temperature.");

            List<TicketEvent> history = service.eventsFor(student, complaint.getId());
            require(history.stream().anyMatch(event -> event.getEventType() == TicketEventType.COMMENT_ADDED),
                "Comments must appear in activity history");
            require(history.stream().anyMatch(event -> event.getEventType() == TicketEventType.TECHNICIAN_ASSIGNED),
                "Assignment must appear in activity history");
            require(history.stream().anyMatch(event -> event.getEventType() == TicketEventType.PROGRESS_UPDATED),
                "Progress notes must appear in activity history");
            require(history.stream().anyMatch(event -> event.getEventType() == TicketEventType.STATUS_CHANGED),
                "Status changes must appear in activity history");
            TicketEvent resolved = history.stream()
                .filter(event -> event.getEventType() == TicketEventType.TICKET_RESOLVED)
                .findFirst().orElseThrow();
            require(resolved.getNote().contains("cooling fan"), "Resolution event must retain its note");
            require(isChronological(history), "Activity history must be chronological");

            int historySize = history.size();
            service.eventsFor(student, complaint.getId());
            service.eventsFor(student, complaint.getId());
            require(service.eventsFor(student, complaint.getId()).size() == historySize,
                "Reading or refreshing history must not create duplicate events");
            FileTicketEventRepository reloadedEvents = new FileTicketEventRepository(eventFile);
            require(reloadedEvents.findByComplaintId(complaint.getId()).size() == historySize,
                "Event history must survive repository reload");
            require(new FileTicketRepository(complaintFile).findAll().get(0).getStatus() == Status.RESOLVED,
                "Complaint state must survive repository reload");

            testChronologicalFileOrdering(directory);
            testLegacyV1Compatibility(directory);
            testMalformedEventFailure(directory);
            testPartialWriteFailureIsReported(directory, users, clock);
            testSqliteWhenAvailable(directory, users, clock);
            System.out.println("PASS: ticket events, comments, progress notes, authorization, resolution notes, chronology, v1 compatibility, event reload, malformed-event handling");
        } finally {
            try (var paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
        }
    }

    private static void testChronologicalFileOrdering(Path directory) {
        FileTicketEventRepository repository = new FileTicketEventRepository(directory.resolve("ordering.tsv"));
        repository.save(new TicketEvent(1, 77, "A01", TicketEventType.COMMENT_ADDED,
            LocalDateTime.of(2026, 11, 21, 12, 0), "Later event"));
        repository.save(new TicketEvent(2, 77, "S01", TicketEventType.COMMENT_ADDED,
            LocalDateTime.of(2026, 11, 21, 10, 0), "Earlier event"));
        List<TicketEvent> ordered = repository.findByComplaintId(77);
        require(ordered.get(0).getNote().equals("Earlier event"),
            "Repository must sort by timestamp rather than insertion order");
    }

    private static void testLegacyV1Compatibility(Path directory) throws Exception {
        Path legacyFile = directory.resolve("legacy-v1.tsv");
        LocalDateTime time = LocalDateTime.of(2026, 1, 5, 9, 15);
        String[] fields = {
            "1442", "S01", "Legacy lighting complaint", "The corridor lights flicker after sunset.",
            "Block A corridor", "ELECTRICAL", "MEDIUM", time.toString(), "OPEN", "", time.toString(), "v1"
        };
        for (int index = 0; index < fields.length; index++) {
            fields[index] = Base64.getEncoder().encodeToString(fields[index].getBytes(StandardCharsets.UTF_8));
        }
        Files.writeString(legacyFile, String.join("\t", fields) + System.lineSeparator(), StandardCharsets.UTF_8);
        List<Complaint> legacy = new FileTicketRepository(legacyFile).findAll();
        require(legacy.size() == 1 && legacy.get(0).getId() == 1442,
            "Existing 12-field v1 complaint records must remain readable");
        FileTicketEventRepository emptyHistory = new FileTicketEventRepository(directory.resolve("legacy-events.tsv"));
        require(emptyHistory.findByComplaintId(1442).isEmpty(),
            "Legacy complaints must not receive invented historical events");
    }

    private static void testMalformedEventFailure(Path directory) throws Exception {
        Path malformed = directory.resolve("malformed-events.tsv");
        Files.writeString(malformed, "not-a-valid-event-record", StandardCharsets.UTF_8);
        try {
            new FileTicketEventRepository(malformed);
            throw new AssertionError("Malformed event data should fail clearly");
        } catch (IllegalStateException expected) {
            require(expected.getMessage().contains("line 1"),
                "Malformed event error must identify the record line");
        }
    }

    private static void testPartialWriteFailureIsReported(Path directory, UserDirectory users, Clock clock) {
        FileTicketRepository tickets = new FileTicketRepository(directory.resolve("partial-write.tsv"));
        TicketEventRepository failingEvents = new TicketEventRepository() {
            @Override public List<TicketEvent> findByComplaintId(int complaintId) { return List.of(); }
            @Override public long nextId() { return 1; }
            @Override public void save(TicketEvent event) { throw new IllegalStateException("simulated event failure"); }
        };
        ComplaintService service = new ComplaintService(
            tickets, failingEvents, users, new WeightedPriorityScorer(), clock);
        try {
            service.create(users.login("student2", "demo123"), "Partial write visibility",
                "This complaint verifies that event-write failures are reported honestly.",
                "Test Lab 2", Category.OTHER, Severity.LOW);
            throw new AssertionError("Expected activity persistence failure");
        } catch (IllegalStateException expected) {
            require(expected.getMessage().contains("was saved")
                    && expected.getMessage().contains("could not be recorded"),
                "Partial-write failure must clearly report the persisted complaint and missing event");
            require(tickets.findAll().size() == 1,
                "Failure test must confirm the complaint write occurred before the event failure");
        }
    }

    private static void testSqliteWhenAvailable(Path directory, UserDirectory users, Clock clock) throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException unavailable) {
            System.out.println("NOT VERIFIED: SQLite ticket-event integration (JDBC driver not on test classpath)");
            return;
        }
        Path database = directory.resolve("events-integration.db");
        SqliteTicketRepository tickets = new SqliteTicketRepository(database);
        SqliteTicketEventRepository events = new SqliteTicketEventRepository(database);
        ComplaintService service = new ComplaintService(
            tickets, events, users, new WeightedPriorityScorer(), clock);
        User student = users.login("student", "demo123");
        Complaint complaint = service.create(student, "SQLite event check",
            "This complaint verifies SQLite ticket event persistence.", "Test Lab 1",
            Category.OTHER, Severity.LOW);
        require(new SqliteTicketEventRepository(database).findByComplaintId(complaint.getId()).size() == 1,
            "SQLite event must survive repository reload");
        System.out.println("PASS: SQLite ticket-event integration");
    }

    private static boolean isChronological(List<TicketEvent> events) {
        for (int index = 1; index < events.size(); index++) {
            TicketEvent previous = events.get(index - 1);
            TicketEvent current = events.get(index);
            if (previous.getTimestamp().isAfter(current.getTimestamp())) return false;
            if (previous.getTimestamp().equals(current.getTimestamp())
                && previous.getEventId() > current.getEventId()) return false;
        }
        return true;
    }

    private static void expectValidation(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected validation failure");
        } catch (ValidationException expected) {
            // Expected.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
