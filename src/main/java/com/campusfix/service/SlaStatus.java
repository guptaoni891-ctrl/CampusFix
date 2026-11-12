package com.campusfix.service;

/** Current or final SLA classification. */
public enum SlaStatus {
    OVERDUE,
    DUE_SOON,
    ON_TRACK,
    RESOLVED_BREACHED,
    RESOLVED_MET,
    RESOLVED_UNKNOWN
}
