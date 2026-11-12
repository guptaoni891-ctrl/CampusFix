package com.campusfix.util;

import com.campusfix.model.Complaint;
import com.campusfix.service.ComplaintService;
import com.campusfix.service.PriorityAssessment;
import com.campusfix.service.PriorityScorer;
import com.campusfix.service.SlaAssessment;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/** CSV export using the same calculated metrics shown by the application service. */
public final class CsvExporter {
    private CsvExporter() {
    }

    public static void export(Path file, List<Complaint> tickets, ComplaintService service) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("ID,Title,Category,Severity,Location,Status,Student ID,Technician ID,"
                + "Priority,Priority Explanation,SLA Deadline,SLA Status,SLA Remaining,Created At,Updated At");
            writer.newLine();
            for (Complaint ticket : tickets) {
                PriorityAssessment priority = service.priorityFor(ticket);
                SlaAssessment sla = service.slaFor(ticket);
                write(writer, Arrays.asList(
                    String.valueOf(ticket.getId()), ticket.getTitle(), ticket.getCategory().name(),
                    ticket.getSeverity().name(), ticket.getLocation(), ticket.getStatus().name(),
                    ticket.getStudentId(), ticket.getTechnicianId() == null ? "" : ticket.getTechnicianId(),
                    String.valueOf(priority.score()), priority.explanation(), sla.deadline().toString(),
                    sla.status().name(), sla.remainingText(), ticket.getCreatedAt().toString(),
                    ticket.getUpdatedAt().toString()));
            }
        }
    }

    /** Backwards-compatible export for callers that only provide a priority strategy. */
    public static void export(Path file, List<Complaint> tickets, PriorityScorer scorer) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("ID,Title,Category,Severity,Location,Status,Student ID,Technician ID,Priority,Created At,Updated At");
            writer.newLine();
            for (Complaint ticket : tickets) {
                write(writer, Arrays.asList(
                    String.valueOf(ticket.getId()), ticket.getTitle(), ticket.getCategory().name(),
                    ticket.getSeverity().name(), ticket.getLocation(), ticket.getStatus().name(),
                    ticket.getStudentId(), ticket.getTechnicianId() == null ? "" : ticket.getTechnicianId(),
                    String.valueOf(scorer.score(ticket, now)), ticket.getCreatedAt().toString(),
                    ticket.getUpdatedAt().toString()));
            }
        }
    }

    private static void write(BufferedWriter writer, List<String> cells) throws IOException {
        writer.write(String.join(",", cells.stream().map(CsvExporter::csv).toList()));
        writer.newLine();
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
