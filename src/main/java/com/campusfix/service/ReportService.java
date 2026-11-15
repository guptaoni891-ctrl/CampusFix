package com.campusfix.service;

import com.campusfix.model.Role;
import com.campusfix.model.Complaint;
import com.campusfix.model.User;
import com.campusfix.util.CsvExporter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Authorization-aware reporting facade used by background Swing export tasks. */
public final class ReportService {
    private final ComplaintService complaintService;
    private final ComplaintQueryService queryService;
    private final AnalyticsService analyticsService;

    public ReportService(ComplaintService complaintService, ComplaintQueryService queryService,
                         AnalyticsService analyticsService) {
        this.complaintService = Objects.requireNonNull(complaintService, "complaintService");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService");
    }

    public void export(User actor, Path file, ReportType type, ComplaintFilter filter) throws IOException {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(type, "type");
        ComplaintFilter criteria = filter == null ? ComplaintFilter.none() : filter;
        switch (type) {
            case ALL_COMPLAINTS -> CsvExporter.export(
                file, queryService.query(actor, ComplaintFilter.none(), ComplaintSort.PRIORITY_DESC),
                complaintService);
            case FILTERED_COMPLAINTS -> CsvExporter.export(
                file, queryService.query(actor, criteria, ComplaintSort.PRIORITY_DESC), complaintService);
            case SLA_SUMMARY -> {
                requireAdministrator(actor);
                CsvExporter.exportSlaSummary(file, analyticsService.analyze(actor, criteria));
            }
            case TECHNICIAN_SUMMARY -> {
                requireAdministrator(actor);
                CsvExporter.exportTechnicianSummary(file, analyticsService.analyze(actor, criteria));
            }
        }
    }

    private void requireAdministrator(User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ValidationException("Only administrators may export analytics summaries.");
        }
    }

    /** Export current table rows in display order, checking visibility again at write time. */
    public void exportVisibleSelection(User actor, Path file, List<Integer> complaintIds) throws IOException {
        Map<Integer, Complaint> visible = complaintService.visibleTo(actor).stream()
            .collect(Collectors.toMap(Complaint::getId, Function.identity()));
        List<Complaint> selected = complaintIds.stream().distinct().map(visible::get)
            .filter(Objects::nonNull).toList();
        CsvExporter.export(file, selected, complaintService);
    }
}
