package com.campusfix.service;

import com.campusfix.model.Complaint;
import com.campusfix.model.Status;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Calculates calendar-time SLA state without mutating complaint records. */
public final class SlaService {
    private final SlaPolicy policy;
    private final Clock clock;

    public SlaService(SlaPolicy policy, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public SlaAssessment assess(Complaint complaint, LocalDateTime reliableResolvedAt) {
        Objects.requireNonNull(complaint, "complaint");
        LocalDateTime deadline = complaint.getCreatedAt().plus(policy.targetFor(complaint.getSeverity()));
        if (complaint.getStatus() == Status.RESOLVED) {
            if (reliableResolvedAt == null) {
                return new SlaAssessment(deadline, null, SlaStatus.RESOLVED_UNKNOWN, null);
            }
            Duration remaining = Duration.between(reliableResolvedAt, deadline);
            SlaStatus status = remaining.isNegative()
                ? SlaStatus.RESOLVED_BREACHED : SlaStatus.RESOLVED_MET;
            return new SlaAssessment(deadline, remaining, status, reliableResolvedAt);
        }
        Duration remaining = Duration.between(LocalDateTime.now(clock), deadline);
        SlaStatus status;
        if (remaining.isNegative()) {
            status = SlaStatus.OVERDUE;
        } else if (remaining.compareTo(policy.dueSoonWindowFor(complaint.getSeverity())) <= 0) {
            status = SlaStatus.DUE_SOON;
        } else {
            status = SlaStatus.ON_TRACK;
        }
        return new SlaAssessment(deadline, remaining, status, null);
    }
}
