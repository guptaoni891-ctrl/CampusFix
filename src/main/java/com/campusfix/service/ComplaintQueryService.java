package com.campusfix.service;

import com.campusfix.model.Complaint;
import com.campusfix.model.User;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Applies reusable filtering and sorting rules outside Swing components. */
public final class ComplaintQueryService {
    private final ComplaintService complaintService;

    public ComplaintQueryService(ComplaintService complaintService) {
        this.complaintService = Objects.requireNonNull(complaintService, "complaintService");
    }

    public List<Complaint> query(User actor, ComplaintFilter filter, ComplaintSort sort) {
        return apply(complaintService.visibleTo(actor), filter, sort);
    }

    public List<Complaint> apply(List<Complaint> source, ComplaintFilter filter, ComplaintSort sort) {
        Objects.requireNonNull(source, "source");
        ComplaintFilter criteria = filter == null ? ComplaintFilter.none() : filter;
        ComplaintSort ordering = sort == null ? ComplaintSort.PRIORITY_DESC : sort;
        return source.stream()
            .filter(complaint -> matches(complaint, criteria))
            .sorted(comparator(ordering))
            .map(Complaint::copy)
            .toList();
    }

    private boolean matches(Complaint complaint, ComplaintFilter filter) {
        if (filter.status() != null && complaint.getStatus() != filter.status()) return false;
        if (filter.severity() != null && complaint.getSeverity() != filter.severity()) return false;
        if (filter.category() != null && complaint.getCategory() != filter.category()) return false;
        LocalDate created = complaint.getCreatedAt().toLocalDate();
        if (filter.createdFrom() != null && created.isBefore(filter.createdFrom())) return false;
        if (filter.createdTo() != null && created.isAfter(filter.createdTo())) return false;
        if (filter.slaStatus() != null
            && complaintService.slaFor(complaint).status() != filter.slaStatus()) return false;
        if (filter.technicianId() != null) {
            if (ComplaintFilter.UNASSIGNED.equals(filter.technicianId())) {
                if (complaint.getTechnicianId() != null) return false;
            } else if (!filter.technicianId().equals(complaint.getTechnicianId())) {
                return false;
            }
        }
        if (filter.searchText() != null) {
            String search = filter.searchText().toLowerCase(Locale.ROOT);
            String text = (complaint.getId() + " " + complaint.getTitle() + " "
                + complaint.getLocation()).toLowerCase(Locale.ROOT);
            if (!text.contains(search)) return false;
        }
        return true;
    }

    private Comparator<Complaint> comparator(ComplaintSort sort) {
        return switch (sort) {
            case PRIORITY_DESC -> Comparator.comparing(complaintService::priorityFor).reversed()
                .thenComparing(Complaint::getCreatedAt);
            case CREATED_NEWEST -> Comparator.comparing(Complaint::getCreatedAt).reversed()
                .thenComparingInt(Complaint::getId);
            case DEADLINE_EARLIEST -> Comparator.comparing(complaintService::slaDeadlineFor)
                .thenComparingInt(Complaint::getId);
            case STATUS -> Comparator.comparing(Complaint::getStatus)
                .thenComparing(complaintService::priorityFor, Comparator.reverseOrder());
        };
    }
}
