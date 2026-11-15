package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Role;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.Technician;
import com.campusfix.model.User;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;

/** Central, authorization-aware analytics aggregation service. */
public final class AnalyticsService {
    private final ComplaintService complaintService;
    private final ComplaintQueryService queryService;
    private final UserDirectory directory;

    public AnalyticsService(ComplaintService complaintService, ComplaintQueryService queryService,
                            UserDirectory directory) {
        this.complaintService = Objects.requireNonNull(complaintService, "complaintService");
        this.queryService = Objects.requireNonNull(queryService, "queryService");
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public AnalyticsSnapshot analyze(User actor, ComplaintFilter filter) {
        requireAuthorizedRole(actor);
        ComplaintFilter criteria = filter == null ? ComplaintFilter.none() : filter;
        List<Complaint> complaints = queryService.query(actor, criteria, ComplaintSort.CREATED_NEWEST);
        return aggregate(actor, complaints, criteria);
    }

    private AnalyticsSnapshot aggregate(User actor, List<Complaint> complaints, ComplaintFilter filter) {
        Map<Status, Long> statuses = enumCounts(Status.values());
        Map<Category, Long> categories = enumCounts(Category.values());
        Map<Severity, Long> severities = enumCounts(Severity.values());
        Map<LocalDate, Long> trend = new TreeMap<>();
        List<Duration> resolutionDurations = new ArrayList<>();
        Map<Integer, SlaAssessment> slaByComplaint = new HashMap<>();
        long overdue = 0;
        long dueSoon = 0;
        long slaMet = 0;
        long slaBreached = 0;

        for (Complaint complaint : complaints) {
            increment(statuses, complaint.getStatus());
            increment(categories, complaint.getCategory());
            increment(severities, complaint.getSeverity());
            trend.merge(complaint.getCreatedAt().toLocalDate(), 1L, Long::sum);
            SlaAssessment sla = complaintService.slaFor(complaint);
            slaByComplaint.put(complaint.getId(), sla);
            if (sla.status() == SlaStatus.OVERDUE) overdue++;
            if (sla.status() == SlaStatus.DUE_SOON) dueSoon++;
            if (sla.status() == SlaStatus.RESOLVED_MET) slaMet++;
            if (sla.status() == SlaStatus.RESOLVED_BREACHED) slaBreached++;
            reliableDuration(complaint, sla).ifPresent(resolutionDurations::add);
        }

        long resolved = statuses.get(Status.RESOLVED);
        OptionalDouble resolutionRate = percentage(resolved, complaints.size());
        long knownSla = slaMet + slaBreached;
        OptionalDouble complianceRate = percentage(slaMet, knownSla);
        return new AnalyticsSnapshot(
            complaints.size(), statuses, resolutionRate, overdue, dueSoon,
            average(resolutionDurations), categories, severities,
            technicianPerformance(actor, complaints, slaByComplaint), knownSla, slaMet, slaBreached,
            complianceRate, new LinkedHashMap<>(trend), filter);
    }

    private List<TechnicianPerformance> technicianPerformance(
            User actor, List<Complaint> complaints, Map<Integer, SlaAssessment> slaByComplaint) {
        List<Technician> technicians = actor.getRole() == Role.ADMIN
            ? directory.technicians()
            : directory.technicians().stream().filter(tech -> tech.getId().equals(actor.getId())).toList();
        List<TechnicianPerformance> result = new ArrayList<>();
        for (Technician technician : technicians) {
            List<Complaint> assigned = complaints.stream()
                .filter(complaint -> technician.getId().equals(complaint.getTechnicianId())).toList();
            long active = assigned.stream().filter(complaint -> complaint.getStatus() != Status.RESOLVED).count();
            long resolved = assigned.stream().filter(complaint -> complaint.getStatus() == Status.RESOLVED).count();
            long overdue = assigned.stream().filter(complaint -> complaint.getStatus() != Status.RESOLVED)
                .map(complaint -> slaByComplaint.get(complaint.getId()))
                .filter(sla -> sla != null && sla.status() == SlaStatus.OVERDUE).count();
            List<Duration> durations = assigned.stream()
                .map(complaint -> reliableDuration(complaint, slaByComplaint.get(complaint.getId())))
                .flatMap(Optional::stream).toList();
            result.add(new TechnicianPerformance(technician, active, resolved, average(durations), overdue));
        }
        return result;
    }

    private Optional<Duration> reliableDuration(Complaint complaint, SlaAssessment sla) {
        if (sla == null || sla.resolvedAt() == null) return Optional.empty();
        Duration duration = Duration.between(complaint.getCreatedAt(), sla.resolvedAt());
        return duration.isNegative() ? Optional.empty() : Optional.of(duration);
    }

    private void requireAuthorizedRole(User actor) {
        if (actor == null) throw new ValidationException("A signed-in user is required.");
        if (actor.getRole() == Role.STUDENT) {
            throw new ValidationException("Analytics are available only to administrators and technicians.");
        }
    }

    private static <E extends Enum<E>> Map<E, Long> enumCounts(E[] values) {
        Map<E, Long> counts = new EnumMap<>(values[0].getDeclaringClass());
        for (E value : values) counts.put(value, 0L);
        return counts;
    }

    private static <E> void increment(Map<E, Long> counts, E key) {
        counts.put(key, counts.getOrDefault(key, 0L) + 1L);
    }

    private static OptionalDouble percentage(long numerator, long denominator) {
        return denominator == 0 ? OptionalDouble.empty()
            : OptionalDouble.of(numerator * 100.0 / denominator);
    }

    private static Optional<Duration> average(List<Duration> durations) {
        if (durations.isEmpty()) return Optional.empty();
        long totalSeconds = durations.stream().mapToLong(Duration::getSeconds).sum();
        return Optional.of(Duration.ofSeconds(totalSeconds / durations.size()));
    }
}
