package com.campusfix.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable calculated SLA deadline, remaining duration, and compliance classification. */
public record SlaAssessment(
        LocalDateTime deadline,
        Duration timeRemaining,
        SlaStatus status,
        LocalDateTime resolvedAt) implements Comparable<SlaAssessment> {

    public SlaAssessment {
        deadline = Objects.requireNonNull(deadline, "deadline");
        status = Objects.requireNonNull(status, "status");
    }

    public boolean isOverdue() {
        return status == SlaStatus.OVERDUE || status == SlaStatus.RESOLVED_BREACHED;
    }

    public boolean isDueSoon() {
        return status == SlaStatus.DUE_SOON;
    }

    public String displayText() {
        return switch (status) {
            case OVERDUE -> "Overdue " + durationLabel(timeRemaining == null ? Duration.ZERO : timeRemaining.abs());
            case DUE_SOON -> "Due in " + durationLabel(timeRemaining);
            case ON_TRACK -> "Due in " + durationLabel(timeRemaining);
            case RESOLVED_MET -> "Resolved within SLA";
            case RESOLVED_BREACHED -> "Resolved after SLA";
            case RESOLVED_UNKNOWN -> "Compliance unavailable";
        };
    }

    public String remainingText() {
        if (timeRemaining == null) return "Not available";
        if (status == SlaStatus.RESOLVED_MET) return durationLabel(timeRemaining) + " early";
        if (status == SlaStatus.RESOLVED_BREACHED) return durationLabel(timeRemaining.abs()) + " late";
        if (status == SlaStatus.OVERDUE) return durationLabel(timeRemaining.abs()) + " overdue";
        return durationLabel(timeRemaining) + " remaining";
    }

    @Override
    public int compareTo(SlaAssessment other) {
        int byUrgency = Integer.compare(urgencyRank(status), urgencyRank(other.status));
        if (byUrgency != 0) return byUrgency;
        if (timeRemaining == null && other.timeRemaining == null) return 0;
        if (timeRemaining == null) return 1;
        if (other.timeRemaining == null) return -1;
        return timeRemaining.compareTo(other.timeRemaining);
    }

    private static int urgencyRank(SlaStatus status) {
        return switch (status) {
            case OVERDUE -> 0;
            case DUE_SOON -> 1;
            case ON_TRACK -> 2;
            case RESOLVED_BREACHED -> 3;
            case RESOLVED_MET -> 4;
            case RESOLVED_UNKNOWN -> 5;
        };
    }

    private static String durationLabel(Duration duration) {
        if (duration == null) return "unknown";
        long minutes = Math.max(0, duration.toMinutes());
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        if (hours >= 48) return (hours / 24) + "d " + (hours % 24) + "h";
        if (hours > 0) return hours + "h " + remainingMinutes + "m";
        return remainingMinutes + "m";
    }
}
