package com.campusfix.service;

import com.campusfix.model.Technician;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Factual technician workload and completion metrics; no artificial performance score. */
public record TechnicianPerformance(
        Technician technician,
        long activeTickets,
        long resolvedTickets,
        Optional<Duration> averageResolutionDuration,
        long overdueActiveTickets) {

    public TechnicianPerformance {
        technician = Objects.requireNonNull(technician, "technician");
        averageResolutionDuration = Objects.requireNonNull(averageResolutionDuration,
            "averageResolutionDuration");
        if (activeTickets < 0 || resolvedTickets < 0 || overdueActiveTickets < 0) {
            throw new IllegalArgumentException("Technician counts cannot be negative.");
        }
    }
}
