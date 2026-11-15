package com.campusfix.util;

import com.campusfix.model.Complaint;
import com.campusfix.service.AnalyticsSnapshot;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.PriorityAssessment;
import com.campusfix.service.PriorityScorer;
import com.campusfix.service.SlaAssessment;
import com.campusfix.service.TechnicianPerformance;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/** CSV export using the same calculated metrics shown by the application service. */
public final class CsvExporter {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private CsvExporter() {
    }

    public static void export(Path file, List<Complaint> tickets, ComplaintService service) throws IOException {
        try (BufferedWriter writer = newReport(file)) {
            writer.write("ID,Title,Description,Category,Severity,Location,Status,Student ID,Technician ID,"
                + "Priority,Priority Explanation,SLA Deadline,SLA Status,SLA Remaining,Created At,Updated At");
            writer.newLine();
            for (Complaint ticket : tickets) {
                PriorityAssessment priority = service.priorityFor(ticket);
                SlaAssessment sla = service.slaFor(ticket);
                write(writer, Arrays.asList(
                    String.valueOf(ticket.getId()), ticket.getTitle(), ticket.getDescription(), ticket.getCategory().name(),
                    ticket.getSeverity().name(), ticket.getLocation(), ticket.getStatus().name(),
                    ticket.getStudentId(), ticket.getTechnicianId() == null ? "" : ticket.getTechnicianId(),
                    String.valueOf(priority.score()), priority.explanation(), TIMESTAMP.format(sla.deadline()),
                    sla.status().name(), sla.remainingText(), TIMESTAMP.format(ticket.getCreatedAt()),
                    TIMESTAMP.format(ticket.getUpdatedAt())));
            }
        }
    }

    public static void exportSlaSummary(Path file, AnalyticsSnapshot analytics) throws IOException {
        try (BufferedWriter writer = newReport(file)) {
            write(writer, List.of("Metric", "Value"));
            write(writer, List.of("Filtered complaints", String.valueOf(analytics.totalComplaints())));
            write(writer, List.of("Overdue unresolved", String.valueOf(analytics.overdueUnresolved())));
            write(writer, List.of("Due soon unresolved", String.valueOf(analytics.dueSoonUnresolved())));
            write(writer, List.of("Known resolved SLA outcomes", String.valueOf(analytics.knownSlaOutcomes())));
            write(writer, List.of("Resolved within SLA", String.valueOf(analytics.slaMet())));
            write(writer, List.of("Resolved after SLA", String.valueOf(analytics.slaBreached())));
            write(writer, List.of("SLA compliance rate",
                analytics.slaComplianceRate().isPresent()
                    ? formatPercent(analytics.slaComplianceRate().getAsDouble()) : "Not available"));
        }
    }

    public static void exportTechnicianSummary(Path file, AnalyticsSnapshot analytics) throws IOException {
        try (BufferedWriter writer = newReport(file)) {
            write(writer, List.of("Technician ID", "Technician", "Specialization", "Active tickets",
                "Resolved tickets", "Average resolution time", "Overdue active tickets"));
            for (TechnicianPerformance performance : analytics.technicianPerformance()) {
                write(writer, List.of(
                    performance.technician().getId(), performance.technician().getName(),
                    performance.technician().getSpecialty(), String.valueOf(performance.activeTickets()),
                    String.valueOf(performance.resolvedTickets()),
                    performance.averageResolutionDuration().map(CsvExporter::durationText)
                        .orElse("Not available"),
                    String.valueOf(performance.overdueActiveTickets())));
            }
        }
    }

    /** Backwards-compatible export for callers that only provide a priority strategy. */
    public static void export(Path file, List<Complaint> tickets, PriorityScorer scorer) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        try (BufferedWriter writer = newReport(file)) {
            writer.write("ID,Title,Description,Category,Severity,Location,Status,Student ID,Technician ID,Priority,Created At,Updated At");
            writer.newLine();
            for (Complaint ticket : tickets) {
                write(writer, Arrays.asList(
                    String.valueOf(ticket.getId()), ticket.getTitle(), ticket.getDescription(), ticket.getCategory().name(),
                    ticket.getSeverity().name(), ticket.getLocation(), ticket.getStatus().name(),
                    ticket.getStudentId(), ticket.getTechnicianId() == null ? "" : ticket.getTechnicianId(),
                    String.valueOf(scorer.score(ticket, now)), TIMESTAMP.format(ticket.getCreatedAt()),
                    TIMESTAMP.format(ticket.getUpdatedAt())));
            }
        }
    }

    private static BufferedWriter newReport(Path file) throws IOException {
        return Files.newBufferedWriter(file, StandardCharsets.UTF_8,
            StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    private static void write(BufferedWriter writer, List<String> cells) throws IOException {
        writer.write(String.join(",", cells.stream().map(CsvExporter::csv).toList()));
        writer.newLine();
    }

    private static String csv(String value) {
        String protectedValue = protectFromFormula(value == null ? "" : value);
        return "\"" + protectedValue.replace("\"", "\"\"") + "\"";
    }

    private static String protectFromFormula(String value) {
        String stripped = value.stripLeading();
        if (stripped.isEmpty()) return value;
        char first = stripped.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@'
            ? "'" + value : value;
    }

    private static String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", value);
    }

    private static String durationText(java.time.Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }
}
