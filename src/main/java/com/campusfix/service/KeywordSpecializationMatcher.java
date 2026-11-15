package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Technician;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Deterministic keyword matcher for the existing free-text technician specialty field. */
public final class KeywordSpecializationMatcher implements SpecializationMatcher {
    private static final Map<Category, List<String>> KEYWORDS = Map.of(
        Category.ELECTRICAL, List.of("electrical", "electric", "power"),
        Category.PLUMBING, List.of("plumbing", "water", "pipe"),
        Category.INTERNET, List.of("internet", "network", "wifi"),
        Category.CLEANING, List.of("cleaning", "facilities"),
        Category.FURNITURE, List.of("furniture", "carpentry", "facilities"),
        Category.OTHER, List.of("general", "maintenance", "facilities")
    );

    @Override
    public int matchScore(Technician technician, Category category) {
        Objects.requireNonNull(technician, "technician");
        Objects.requireNonNull(category, "category");
        String specialty = technician.getSpecialty().toLowerCase(Locale.ROOT);
        return KEYWORDS.get(category).stream().anyMatch(specialty::contains) ? 100 : 0;
    }

    @Override
    public String explanation(Technician technician, Category category) {
        if (matchScore(technician, category) > 0) {
            return technician.getSpecialty() + " matches the " + friendly(category) + " category.";
        }
        return "No direct " + friendly(category) + " specialty match; ranked as a workload fallback.";
    }

    private static String friendly(Category category) {
        String value = category.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
