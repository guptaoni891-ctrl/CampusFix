package com.campusfix.model;

/** Significant actions recorded in a complaint's permanent activity timeline. */
public enum TicketEventType {
    TICKET_CREATED,
    TECHNICIAN_ASSIGNED,
    TECHNICIAN_REASSIGNED,
    STATUS_CHANGED,
    COMMENT_ADDED,
    PROGRESS_UPDATED,
    TICKET_RESOLVED
}
