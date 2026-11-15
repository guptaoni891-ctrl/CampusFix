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
import com.campusfix.persistence.TicketRepository;
import com.campusfix.service.AnalyticsService;
import com.campusfix.service.AnalyticsSnapshot;
import com.campusfix.service.ComplaintFilter;
import com.campusfix.service.ComplaintQueryService;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.ComplaintSort;
import com.campusfix.service.ReportService;
import com.campusfix.service.ReportType;
import com.campusfix.service.SlaStatus;
import com.campusfix.service.UserDirectory;
import com.campusfix.service.ValidationException;
import com.campusfix.service.WeightedPriorityScorer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

/** Focused analytics, combined-filtering, authorization, and safe-reporting tests. */
public final class AnalyticsReportingTest {
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-11-20T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    private AnalyticsReportingTest() {
    }

    public static void run() throws Exception {
        Path directory = Files.createTempDirectory("campusfix-analytics-test-");
        try {
            UserDirectory users = new UserDirectory();
            FileTicketRepository tickets = new FileTicketRepository(directory.resolve("complaints.tsv"));
            FileTicketEventRepository events = new FileTicketEventRepository(directory.resolve("events.tsv"));
            populate(tickets, events);
            TestServices services = services(tickets, events, users);
            User admin = users.login("admin", "demo123");
            User student = users.login("student", "demo123");
            User technician = users.login("tech", "demo123");

            testAggregations(services.analytics(), admin);
            testCombinedFiltersAndSorting(services, admin);
            testAuthorizationAndTechnicianVisibility(services.analytics(), student, technician);
            testSafeCsvReports(directory, services, admin, student);
            testEmptyDataset(directory, users, admin);
            testFileReload(directory, users, admin);
            testSqliteConsistencyWhenAvailable(directory, users, admin,
                services.analytics().analyze(admin, ComplaintFilter.none()));
            System.out.println("PASS: analytics aggregation, empty/missing history, combined filters, sorting, authorization, safe CSV reports");
        } finally {
            deleteTree(directory);
        }
    }

    private static void testAggregations(AnalyticsService analytics, User admin) {
        AnalyticsSnapshot snapshot = analytics.analyze(admin, ComplaintFilter.none());
        require(snapshot.totalComplaints() == 7, "Total complaint aggregation");
        require(snapshot.count(Status.OPEN) == 1, "Open count");
        require(snapshot.count(Status.ASSIGNED) == 2, "Assigned count");
        require(snapshot.count(Status.IN_PROGRESS) == 1, "In-progress count");
        require(snapshot.count(Status.RESOLVED) == 3, "Resolved count");
        require(snapshot.resolutionRate().isPresent()
            && close(snapshot.resolutionRate().getAsDouble(), 42.857), "Resolution rate");
        require(snapshot.overdueUnresolved() == 1, "Overdue unresolved count");
        require(snapshot.dueSoonUnresolved() == 1, "Due-soon unresolved count");
        require(snapshot.averageResolutionDuration().isPresent()
            && snapshot.averageResolutionDuration().get().equals(Duration.ofMinutes(1110)),
            "Average resolution duration must use only reliable timestamps");
        require(snapshot.knownSlaOutcomes() == 2 && snapshot.slaMet() == 1
            && snapshot.slaBreached() == 1, "Known SLA outcome counts");
        require(snapshot.slaComplianceRate().isPresent()
            && snapshot.slaComplianceRate().getAsDouble() == 50.0,
            "Unknown legacy SLA outcomes must be excluded from compliance rate");
        require(snapshot.categoryCounts().get(Category.ELECTRICAL) == 2,
            "Category distribution");
        require(snapshot.severityCounts().get(Severity.HIGH) == 3,
            "Severity distribution");
        require(snapshot.creationTrend().size() >= 3, "Creation trend aggregation");
        require(snapshot.technicianPerformance().stream()
            .filter(item -> item.technician().getId().equals("T01"))
            .anyMatch(item -> item.activeTickets() == 2 && item.resolvedTickets() == 2
                && item.overdueActiveTickets() == 1
                && item.averageResolutionDuration().isPresent()),
            "Technician active, resolved, overdue, and duration metrics");
    }

    private static void testCombinedFiltersAndSorting(TestServices services, User admin) {
        LocalDate boundary = NOW.minusHours(30).toLocalDate();
        ComplaintFilter combined = new ComplaintFilter(
            Status.RESOLVED, Severity.HIGH, Category.INTERNET,
            boundary, boundary, SlaStatus.RESOLVED_BREACHED, "T01", "network core");
        List<Complaint> filtered = services.query().query(admin, combined, ComplaintSort.PRIORITY_DESC);
        require(filtered.size() == 1 && filtered.get(0).getId() == 5,
            "Multiple filters must combine with inclusive date boundaries");
        AnalyticsSnapshot filteredAnalytics = services.analytics().analyze(admin, combined);
        require(filteredAnalytics.totalComplaints() == 1
            && filteredAnalytics.slaBreached() == 1,
            "Analytics must consistently reflect the selected filters");
        expectValidation(() -> new ComplaintFilter(null, null, null,
            LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 20), null, null, null));

        List<Complaint> newest = services.query().query(admin, ComplaintFilter.none(),
            ComplaintSort.CREATED_NEWEST);
        require(newest.get(0).getId() == 7, "Creation-date sorting");
        List<Complaint> deadline = services.query().query(admin, ComplaintFilter.none(),
            ComplaintSort.DEADLINE_EARLIEST);
        require(deadline.get(0).getId() == 6, "SLA deadline sorting");
        List<Complaint> priority = services.query().query(admin, ComplaintFilter.none(),
            ComplaintSort.PRIORITY_DESC);
        require(services.complaints().score(priority.get(0)) >= services.complaints().score(priority.get(1)),
            "Priority sorting");
        List<Complaint> workflow = services.query().query(admin, ComplaintFilter.none(), ComplaintSort.STATUS);
        require(workflow.get(0).getStatus().ordinal() <= workflow.get(1).getStatus().ordinal(),
            "Status sorting");
    }

    private static void testAuthorizationAndTechnicianVisibility(AnalyticsService analytics,
                                                                  User student, User technician) {
        expectValidation(() -> analytics.analyze(student, ComplaintFilter.none()));
        AnalyticsSnapshot technicianSnapshot = analytics.analyze(technician, ComplaintFilter.none());
        require(technicianSnapshot.totalComplaints() == 4,
            "Technicians must see analytics only for currently assigned tickets");
        require(technicianSnapshot.technicianPerformance().size() == 1
            && technicianSnapshot.technicianPerformance().get(0).technician().getId().equals(technician.getId()),
            "Technicians must not receive other technicians' performance data");
    }

    private static void testSafeCsvReports(Path directory, TestServices services,
                                           User admin, User student) throws Exception {
        ReportService reports = new ReportService(
            services.complaints(), services.query(), services.analytics());
        Path complaintsCsv = directory.resolve("complaints-report.csv");
        reports.export(admin, complaintsCsv, ReportType.ALL_COMPLAINTS, ComplaintFilter.none());
        String complaintOutput = Files.readString(complaintsCsv);
        require(complaintOutput.contains("Priority Explanation")
            && complaintOutput.contains("SLA Deadline"), "Complaint report headings");
        require(complaintOutput.contains("'=SUM(1,2)"),
            "User-entered spreadsheet formulas must be prefixed safely");
        require(complaintOutput.contains("\"\"quoted\"\""), "CSV quote escaping");
        require(complaintOutput.contains("2026-11-20 12:00:00"), "Consistent timestamp format");
        require(!complaintOutput.contains("demo123"), "Reports must not expose credentials");
        try {
            reports.export(admin, complaintsCsv, ReportType.ALL_COMPLAINTS, ComplaintFilter.none());
            throw new AssertionError("Exports must not overwrite an existing file");
        } catch (java.nio.file.FileAlreadyExistsException expected) {
            require(Files.readString(complaintsCsv).equals(complaintOutput), "Existing file must survive");
        }
        Path selection = directory.resolve("selected.csv");
        User technician = new UserDirectory().login("tech", "demo123");
        reports.exportVisibleSelection(technician, selection, List.of(5, 3, 2));
        String selected = Files.readString(selection);
        require(!selected.contains("Plumbing inspection")
            && selected.indexOf("Resolved network core") >= 0
            && selected.indexOf("Resolved network core") < selected.indexOf("Assigned network access"),
            "Selected export must keep display order and exclude out-of-scope records");

        ComplaintFilter onlyOpen = new ComplaintFilter(Status.OPEN, null, null,
            null, null, null, null, null);
        Path filtered = directory.resolve("filtered.csv");
        reports.export(admin, filtered, ReportType.FILTERED_COMPLAINTS, onlyOpen);
        String filteredOutput = Files.readString(filtered);
        require(filteredOutput.contains("\"7\"") && !filteredOutput.contains("\"1\",\"Critical"),
            "Filtered report must contain only the open complaint");

        Path sla = directory.resolve("sla.csv");
        reports.export(admin, sla, ReportType.SLA_SUMMARY, ComplaintFilter.none());
        require(Files.readString(sla).contains("Known resolved SLA outcomes"),
            "SLA summary headings");
        Path technicians = directory.resolve("technicians.csv");
        reports.export(admin, technicians, ReportType.TECHNICIAN_SUMMARY, ComplaintFilter.none());
        require(Files.readString(technicians).contains("Average resolution time"),
            "Technician summary headings");
        expectValidation(() -> {
            try {
                reports.export(student, sla, ReportType.SLA_SUMMARY, ComplaintFilter.none());
            } catch (java.io.IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private static void testEmptyDataset(Path directory, UserDirectory users, User admin) throws Exception {
        TestServices empty = services(
            new FileTicketRepository(directory.resolve("empty-complaints.tsv")),
            new FileTicketEventRepository(directory.resolve("empty-events.tsv")), users);
        AnalyticsSnapshot snapshot = empty.analytics().analyze(admin, ComplaintFilter.none());
        require(snapshot.totalComplaints() == 0, "Empty total");
        require(snapshot.resolutionRate().isEmpty(), "Empty resolution rate must be unavailable");
        require(snapshot.averageResolutionDuration().isEmpty(), "Empty resolution duration");
        require(snapshot.slaComplianceRate().isEmpty(), "Empty SLA compliance rate");
        Path emptyCsv = directory.resolve("empty.csv");
        new ReportService(empty.complaints(), empty.query(), empty.analytics()).export(
            admin, emptyCsv, ReportType.ALL_COMPLAINTS, ComplaintFilter.none());
        require(Files.readAllLines(emptyCsv).size() == 1,
            "Empty complaint report must still contain a clear header");
    }

    private static void testFileReload(Path directory, UserDirectory users, User admin) {
        TestServices reloaded = services(
            new FileTicketRepository(directory.resolve("complaints.tsv")),
            new FileTicketEventRepository(directory.resolve("events.tsv")), users);
        require(reloaded.analytics().analyze(admin, ComplaintFilter.none()).totalComplaints() == 7,
            "Analytics must remain consistent after v1 file repository reload");
    }

    private static void testSqliteConsistencyWhenAvailable(Path directory, UserDirectory users,
                                                            User admin, AnalyticsSnapshot fileSnapshot) throws Exception {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException unavailable) {
            System.out.println("NOT VERIFIED: SQLite analytics consistency (org.sqlite.JDBC not on test classpath)");
            return;
        }
        Path database = directory.resolve("analytics.db");
        SqliteTicketRepository tickets = new SqliteTicketRepository(database);
        SqliteTicketEventRepository events = new SqliteTicketEventRepository(database);
        populate(tickets, events);
        AnalyticsSnapshot sqlite = services(tickets, events, users).analytics()
            .analyze(admin, ComplaintFilter.none());
        require(sqlite.equals(fileSnapshot),
            "SQLite analytics must match file-storage analytics");
        System.out.println("PASS: SQLite analytics consistency");
    }

    private static TestServices services(TicketRepository tickets, TicketEventRepository events,
                                         UserDirectory users) {
        ComplaintService complaints = new ComplaintService(
            tickets, events, users, new WeightedPriorityScorer(), CLOCK);
        ComplaintQueryService query = new ComplaintQueryService(complaints);
        return new TestServices(complaints, query, new AnalyticsService(complaints, query, users));
    }

    private static void populate(TicketRepository tickets, TicketEventRepository events) {
        save(tickets, complaint(1, "Critical electrical outage", Category.ELECTRICAL,
            Severity.CRITICAL, NOW.minusHours(5), Status.ASSIGNED, "T01"));
        save(tickets, complaint(2, "Assigned network access", Category.INTERNET,
            Severity.HIGH, NOW.minusHours(23), Status.ASSIGNED, "T01"));
        save(tickets, complaint(3, "Plumbing inspection", Category.PLUMBING,
            Severity.MEDIUM, NOW.minusHours(24), Status.IN_PROGRESS, "T02"));
        save(tickets, complaint(4, "Resolved chair repair", Category.FURNITURE,
            Severity.LOW, NOW.minusHours(10), Status.RESOLVED, "T01"));
        save(tickets, complaint(5, "Resolved network core failure", Category.INTERNET,
            Severity.HIGH, NOW.minusHours(30), Status.RESOLVED, "T01"));
        save(tickets, complaint(6, "Legacy resolved cleaning", Category.CLEANING,
            Severity.MEDIUM, NOW.minusDays(5), Status.RESOLVED, "T02"));
        save(tickets, complaint(7, "=SUM(1,2)\n\"quoted\"", Category.ELECTRICAL,
            Severity.HIGH, NOW, Status.OPEN, null));
        events.save(new TicketEvent(1, 4, "T01", TicketEventType.TICKET_RESOLVED,
            NOW.minusHours(2), "Reliable completion"));
        events.save(new TicketEvent(2, 5, "T01", TicketEventType.TICKET_RESOLVED,
            NOW.minusHours(1), "Reliable late completion"));
        events.save(new TicketEvent(3, 6, "T02", TicketEventType.TICKET_RESOLVED,
            NOW.minusDays(6), "Impossible timestamp before the ticket existed"));
    }

    private static Complaint complaint(int id, String title, Category category, Severity severity,
                                       LocalDateTime created, Status status, String technicianId) {
        return new Complaint(id, "S01", title,
            "Synthetic analytics data with comma, quote \" and newline.\nSecond line.",
            id == 7 ? "+Formula Hall" : "Test Building " + id,
            category, severity, created, status, technicianId, created);
    }

    private static void save(TicketRepository repository, Complaint complaint) {
        repository.save(complaint);
    }

    private static void expectValidation(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected validation failure");
        } catch (ValidationException expected) {
            // Expected.
        }
    }

    private static boolean close(double actual, double expected) {
        return Math.abs(actual - expected) < 0.01;
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

    private record TestServices(
            ComplaintService complaints,
            ComplaintQueryService query,
            AnalyticsService analytics) {
    }
}
