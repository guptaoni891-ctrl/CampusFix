package com.campusfix.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable audit event associated with one complaint. */
public final class TicketEvent {
    private final long eventId;
    private final int complaintId;
    private final String actorId;
    private final TicketEventType eventType;
    private final LocalDateTime timestamp;
    private final String note;

    public TicketEvent(long eventId, int complaintId, String actorId, TicketEventType eventType,
                       LocalDateTime timestamp, String note) {
        if (eventId <= 0) throw new IllegalArgumentException("Event ID must be positive.");
        if (complaintId <= 0) throw new IllegalArgumentException("Complaint ID must be positive.");
        if (actorId == null || actorId.isBlank()) throw new IllegalArgumentException("Event actor is required.");
        this.eventId = eventId;
        this.complaintId = complaintId;
        this.actorId = actorId;
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.note = note == null || note.isBlank() ? null : note;
    }

    public long getEventId() { return eventId; }
    public int getComplaintId() { return complaintId; }
    public String getActorId() { return actorId; }
    public TicketEventType getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getNote() { return note; }
}
