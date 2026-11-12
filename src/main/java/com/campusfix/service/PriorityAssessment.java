package com.campusfix.service;

import java.util.Objects;

/** Immutable priority score and its documented component breakdown. */
public record PriorityAssessment(
        int score,
        int severityPoints,
        int categoryPoints,
        int agePoints,
        int operationalPoints,
        String explanation) implements Comparable<PriorityAssessment> {

    public PriorityAssessment {
        if (score < 0 || score > 100) throw new IllegalArgumentException("Priority must be 0-100.");
        if (severityPoints < 0 || categoryPoints < 0 || agePoints < 0 || operationalPoints < 0) {
            throw new IllegalArgumentException("Priority components cannot be negative.");
        }
        explanation = Objects.requireNonNull(explanation, "explanation");
    }

    @Override
    public int compareTo(PriorityAssessment other) {
        return Integer.compare(score, other.score);
    }
}
