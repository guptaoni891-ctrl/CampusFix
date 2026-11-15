package com.campusfix.persistence;

import com.campusfix.model.TicketEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Volatile event storage retained for backwards-compatible service construction and isolated tools. */
public final class InMemoryTicketEventRepository implements TicketEventRepository {
    private final Map<Long, TicketEvent> events = new LinkedHashMap<>();

    @Override
    public synchronized List<TicketEvent> findByComplaintId(int complaintId) {
        return events.values().stream()
            .filter(event -> event.getComplaintId() == complaintId)
            .sorted(Comparator.comparing(TicketEvent::getTimestamp).thenComparingLong(TicketEvent::getEventId))
            .toList();
    }

    @Override
    public synchronized long nextId() {
        return events.keySet().stream().mapToLong(Long::longValue).max().orElse(0L) + 1;
    }

    @Override
    public synchronized void save(TicketEvent event) {
        if (events.containsKey(event.getEventId())) {
            throw new IllegalStateException("Duplicate ticket event ID: " + event.getEventId());
        }
        events.put(event.getEventId(), event);
    }
}
