package com.campusfix.service;

import com.campusfix.model.Severity;
import java.time.Duration;

/** Configurable strategy for calendar-time service-level targets. */
public interface SlaPolicy {
    Duration targetFor(Severity severity);
    Duration dueSoonWindowFor(Severity severity);
}
