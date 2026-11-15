package com.campusfix.service;

/** CSV reports available from the CampusFix administrator workflow. */
public enum ReportType {
    ALL_COMPLAINTS("All complaints"),
    FILTERED_COMPLAINTS("Filtered complaints"),
    SLA_SUMMARY("SLA performance summary"),
    TECHNICIAN_SUMMARY("Technician workload summary");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
