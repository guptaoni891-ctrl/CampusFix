package com.campusfix.persistence;
import com.campusfix.model.Complaint;
import java.util.List;
public interface TicketRepository {
    List<Complaint> findAll();
    int nextId();
    void save(Complaint ticket);
    String mode();
}
