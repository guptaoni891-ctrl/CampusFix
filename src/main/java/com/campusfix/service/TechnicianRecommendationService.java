package com.campusfix.service;

import com.campusfix.model.Complaint;
import com.campusfix.model.Status;
import com.campusfix.model.Technician;
import com.campusfix.persistence.TicketRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Ranks technicians by specialty fit, then active workload, with deterministic tie-breaking. */
public final class TechnicianRecommendationService {
    private final TicketRepository repository;
    private final UserDirectory directory;
    private final SpecializationMatcher matcher;

    public TechnicianRecommendationService(TicketRepository repository, UserDirectory directory,
                                           SpecializationMatcher matcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.matcher = Objects.requireNonNull(matcher, "matcher");
    }

    public List<TechnicianRecommendation> recommendationsFor(Complaint complaint) {
        Objects.requireNonNull(complaint, "complaint");
        Map<String, Long> workload = repository.findAll().stream()
            .filter(ticket -> ticket.getStatus() != Status.RESOLVED && ticket.getTechnicianId() != null)
            .collect(Collectors.groupingBy(Complaint::getTechnicianId, Collectors.counting()));
        return directory.technicians().stream()
            .map(technician -> recommendation(technician, complaint, workload))
            .sorted(Comparator
                .comparingInt(TechnicianRecommendation::specialtyMatchScore).reversed()
                .thenComparingLong(TechnicianRecommendation::activeWorkload)
                .thenComparing(item -> item.technician().getId()))
            .toList();
    }

    private TechnicianRecommendation recommendation(Technician technician, Complaint complaint,
                                                     Map<String, Long> workload) {
        int match = matcher.matchScore(technician, complaint.getCategory());
        long active = workload.getOrDefault(technician.getId(), 0L);
        String explanation = matcher.explanation(technician, complaint.getCategory())
            + " Active workload: " + active + " ticket(s)."
            + (active == 0 ? " Currently unassigned." : "");
        return new TechnicianRecommendation(technician, active, match, explanation);
    }
}
