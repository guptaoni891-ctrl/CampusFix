package com.campusfix.persistence;

import com.campusfix.model.TicketEvent;
import java.util.List;

/** Storage boundary for complaint comments, progress notes, and audit events. */
public interface TicketEventRepository {
    List<TicketEvent> findByComplaintId(int complaintId);
    long nextId();
    void save(TicketEvent event);
}
