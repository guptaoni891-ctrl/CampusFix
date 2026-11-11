package com.campusfix.service;
import com.campusfix.model.Complaint;
import java.time.LocalDateTime;
public interface PriorityScorer {
    int score(Complaint complaint, LocalDateTime now);
}
