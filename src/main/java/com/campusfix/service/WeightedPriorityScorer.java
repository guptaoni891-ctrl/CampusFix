package com.campusfix.service;

import com.campusfix.model.Complaint;
import com.campusfix.model.Status;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Explainable weighted priority strategy.
 *
 * <p>Maximum weights are severity 55, category 15, age 20, and operational urgency 10.
 * Age contributes one point per six complete calendar hours. Operational urgency reflects
 * workflow/SLA pressure without changing persisted complaint data.</p>
 */
public final class WeightedPriorityScorer implements PriorityScorer {
    private final SlaPolicy slaPolicy;

    public WeightedPriorityScorer() {
        this(new StandardSlaPolicy());
    }

    public WeightedPriorityScorer(SlaPolicy slaPolicy) {
        this.slaPolicy = Objects.requireNonNull(slaPolicy, "slaPolicy");
    }

    @Override
    public int score(Complaint complaint, LocalDateTime now) {
        return assess(complaint, now).score();
    }

    @Override
    public PriorityAssessment assess(Complaint complaint, LocalDateTime now) {
        Objects.requireNonNull(complaint, "complaint");
        Objects.requireNonNull(now, "now");
        int severityPoints = switch (complaint.getSeverity()) {
            case LOW -> 10;
            case MEDIUM -> 25;
            case HIGH -> 40;
            case CRITICAL -> 55;
        };
        int categoryPoints = switch (complaint.getCategory()) {
            case ELECTRICAL -> 15;
            case INTERNET, PLUMBING -> 12;
            case CLEANING -> 7;
            case FURNITURE -> 5;
            case OTHER -> 4;
        };
        long ageHours = Math.max(0, Duration.between(complaint.getCreatedAt(), now).toHours());
        int agePoints = (int) Math.min(20, ageHours / 6);
        int operationalPoints = operationalPoints(complaint, now);
        int total = Math.max(0, Math.min(100,
            severityPoints + categoryPoints + agePoints + operationalPoints));
        String explanation = "Severity " + severityPoints + " + category " + categoryPoints
            + " + age " + agePoints + " + operational urgency " + operationalPoints
            + " = " + total + "/100.";
        return new PriorityAssessment(total, severityPoints, categoryPoints, agePoints,
            operationalPoints, explanation);
    }

    private int operationalPoints(Complaint complaint, LocalDateTime now) {
        if (complaint.getStatus() == Status.RESOLVED) return 0;
        LocalDateTime deadline = complaint.getCreatedAt().plus(slaPolicy.targetFor(complaint.getSeverity()));
        Duration remaining = Duration.between(now, deadline);
        if (remaining.isNegative()) return 10;
        if (remaining.compareTo(slaPolicy.dueSoonWindowFor(complaint.getSeverity())) <= 0) return 7;
        return switch (complaint.getStatus()) {
            case OPEN -> 5;
            case ASSIGNED -> 3;
            case IN_PROGRESS -> 2;
            case RESOLVED -> 0;
        };
    }
}
