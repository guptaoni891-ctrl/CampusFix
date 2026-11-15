package com.campusfix.service;

/** Supported complaint ordering options for tables and reports. */
public enum ComplaintSort {
    PRIORITY_DESC("Priority: highest first"),
    CREATED_NEWEST("Created: newest first"),
    DEADLINE_EARLIEST("SLA deadline: earliest first"),
    STATUS("Workflow status");

    private final String displayName;

    ComplaintSort(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
