package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import java.time.LocalDate;

/** Immutable criteria that can be combined for complaint searches and analytics. */
public record ComplaintFilter(
        Status status,
        Severity severity,
        Category category,
        LocalDate createdFrom,
        LocalDate createdTo,
        SlaStatus slaStatus,
        String technicianId,
        String searchText) {

    public static final String UNASSIGNED = "__UNASSIGNED__";

    public ComplaintFilter {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ValidationException("The start date must be on or before the end date.");
        }
        technicianId = normalize(technicianId);
        searchText = normalize(searchText);
    }

    public static ComplaintFilter none() {
        return new ComplaintFilter(null, null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return status == null && severity == null && category == null
            && createdFrom == null && createdTo == null && slaStatus == null
            && technicianId == null && searchText == null;
    }

    private static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }
}
