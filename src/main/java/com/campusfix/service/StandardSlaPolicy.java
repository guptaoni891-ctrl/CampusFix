package com.campusfix.service;

import com.campusfix.model.Severity;
import java.time.Duration;
import java.util.Objects;

/** Coursework SLA policy using calendar time rather than business hours. */
public final class StandardSlaPolicy implements SlaPolicy {
    @Override
    public Duration targetFor(Severity severity) {
        return switch (Objects.requireNonNull(severity, "severity")) {
            case CRITICAL -> Duration.ofHours(4);
            case HIGH -> Duration.ofHours(24);
            case MEDIUM -> Duration.ofHours(72);
            case LOW -> Duration.ofHours(168);
        };
    }

    @Override
    public Duration dueSoonWindowFor(Severity severity) {
        return switch (Objects.requireNonNull(severity, "severity")) {
            case CRITICAL -> Duration.ofHours(1);
            case HIGH -> Duration.ofHours(4);
            case MEDIUM -> Duration.ofHours(12);
            case LOW -> Duration.ofHours(24);
        };
    }
}
