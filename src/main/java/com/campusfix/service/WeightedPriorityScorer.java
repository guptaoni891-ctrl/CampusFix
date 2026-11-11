package com.campusfix.service;
import com.campusfix.model.*;
import java.time.Duration;
import java.time.LocalDateTime;
public final class WeightedPriorityScorer implements PriorityScorer {
    @Override public int score(Complaint complaint,LocalDateTime now){
        int severity = switch(complaint.getSeverity()) {case LOW->10;case MEDIUM->25;case HIGH->45;case CRITICAL->65;};
        int category = switch(complaint.getCategory()) {case ELECTRICAL->15;case INTERNET,PLUMBING->10;default->5;};
        long hours = Math.max(0, Duration.between(complaint.getCreatedAt(),now).toHours());
        int waiting = (int)Math.min(20, hours/12*2);
        return Math.min(100,severity+category+waiting);
    }
}
