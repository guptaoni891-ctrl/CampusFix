package com.campusfix.service;

import com.campusfix.model.Technician;
import java.util.Objects;

/** Explainable technician recommendation with its workload and specialty evidence. */
public record TechnicianRecommendation(
        Technician technician,
        long activeWorkload,
        int specialtyMatchScore,
        String explanation) {

    public TechnicianRecommendation {
        technician = Objects.requireNonNull(technician, "technician");
        if (activeWorkload < 0) throw new IllegalArgumentException("Workload cannot be negative.");
        if (specialtyMatchScore < 0 || specialtyMatchScore > 100) {
            throw new IllegalArgumentException("Specialty score must be 0-100.");
        }
        explanation = Objects.requireNonNull(explanation, "explanation");
    }

    public boolean isSpecialtyMatch() {
        return specialtyMatchScore > 0;
    }
}
