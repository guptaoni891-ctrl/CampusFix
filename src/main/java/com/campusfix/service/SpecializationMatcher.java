package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Technician;

/** Strategy for explaining how well a technician specialty fits a complaint category. */
public interface SpecializationMatcher {
    int matchScore(Technician technician, Category category);
    String explanation(Technician technician, Category category);
}
