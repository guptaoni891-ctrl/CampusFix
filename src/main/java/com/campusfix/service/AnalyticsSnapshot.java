package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Immutable analytics result calculated from one authorized, optionally filtered dataset. */
public record AnalyticsSnapshot(
        int totalComplaints,
        Map<Status, Long> statusCounts,
        OptionalDouble resolutionRate,
        long overdueUnresolved,
        long dueSoonUnresolved,
        Optional<Duration> averageResolutionDuration,
        Map<Category, Long> categoryCounts,
        Map<Severity, Long> severityCounts,
        List<TechnicianPerformance> technicianPerformance,
        long knownSlaOutcomes,
        long slaMet,
        long slaBreached,
        OptionalDouble slaComplianceRate,
        Map<LocalDate, Long> creationTrend,
        ComplaintFilter filter) {

    public AnalyticsSnapshot {
        if (totalComplaints < 0 || overdueUnresolved < 0 || dueSoonUnresolved < 0
            || knownSlaOutcomes < 0 || slaMet < 0 || slaBreached < 0) {
            throw new IllegalArgumentException("Analytics counts cannot be negative.");
        }
        statusCounts = Map.copyOf(statusCounts);
        resolutionRate = Objects.requireNonNull(resolutionRate, "resolutionRate");
        averageResolutionDuration = Objects.requireNonNull(averageResolutionDuration,
            "averageResolutionDuration");
        categoryCounts = Map.copyOf(categoryCounts);
        severityCounts = Map.copyOf(severityCounts);
        technicianPerformance = List.copyOf(technicianPerformance);
        slaComplianceRate = Objects.requireNonNull(slaComplianceRate, "slaComplianceRate");
        creationTrend = Map.copyOf(creationTrend);
        filter = filter == null ? ComplaintFilter.none() : filter;
    }

    public long count(Status status) {
        return statusCounts.getOrDefault(status, 0L);
    }
}
