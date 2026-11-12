package com.campusfix.service;

import com.campusfix.model.Complaint;
import java.time.LocalDateTime;

/** Strategy for deterministic complaint-priority calculations. */
public interface PriorityScorer {
    int score(Complaint complaint, LocalDateTime now);

    /**
     * Returns an explainable assessment. Existing scorer implementations remain source-compatible
     * through this default implementation, while richer scorers can override it.
     */
    default PriorityAssessment assess(Complaint complaint, LocalDateTime now) {
        int score = Math.max(0, Math.min(100, score(complaint, now)));
        return new PriorityAssessment(score, 0, 0, 0, 0,
            "Priority supplied by " + getClass().getSimpleName() + ".");
    }
}
