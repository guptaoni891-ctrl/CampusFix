package com.campusfix.service;

import com.campusfix.model.Category;
import com.campusfix.model.Complaint;
import com.campusfix.model.Role;
import com.campusfix.model.Severity;
import com.campusfix.model.Status;
import com.campusfix.model.Technician;
import com.campusfix.model.TicketEvent;
import com.campusfix.model.TicketEventType;
import com.campusfix.model.User;
import com.campusfix.persistence.InMemoryTicketEventRepository;
import com.campusfix.persistence.TicketEventRepository;
import com.campusfix.persistence.TicketRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/** Application service for complaint workflows, authorization, and activity history. */
public final class ComplaintService {
    private final TicketRepository repository;
    private final TicketEventRepository eventRepository;
    private final UserDirectory directory;
    private final PriorityScorer scorer;
    private final Clock clock;
    private final SlaService slaService;
    private final TechnicianRecommendationService recommendationService;
    private final List<TicketListener> listeners = new ArrayList<>();

    /** Backwards-compatible constructor used by tools that do not require durable event history. */
    public ComplaintService(TicketRepository repository, UserDirectory directory, PriorityScorer scorer) {
        this(repository, new InMemoryTicketEventRepository(), directory, scorer, Clock.systemDefaultZone());
    }

    public ComplaintService(TicketRepository repository, TicketEventRepository eventRepository,
                            UserDirectory directory, PriorityScorer scorer) {
        this(repository, eventRepository, directory, scorer, Clock.systemDefaultZone());
    }

    public ComplaintService(TicketRepository repository, TicketEventRepository eventRepository,
                            UserDirectory directory, PriorityScorer scorer, Clock clock) {
        this(repository, eventRepository, directory, scorer, clock,
            new SlaService(new StandardSlaPolicy(), clock),
            new TechnicianRecommendationService(repository, directory, new KeywordSpecializationMatcher()));
    }

    public ComplaintService(TicketRepository repository, TicketEventRepository eventRepository,
                            UserDirectory directory, PriorityScorer scorer, Clock clock,
                            SlaService slaService,
                            TechnicianRecommendationService recommendationService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository");
        this.directory = Objects.requireNonNull(directory, "directory");
        this.scorer = Objects.requireNonNull(scorer, "scorer");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.slaService = Objects.requireNonNull(slaService, "slaService");
        this.recommendationService = Objects.requireNonNull(recommendationService, "recommendationService");
    }

    public String persistenceMode() { return repository.mode(); }

    public void addListener(TicketListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public int score(Complaint complaint) {
        return priorityFor(complaint).score();
    }

    public PriorityAssessment priorityFor(Complaint complaint) {
        return scorer.assess(Objects.requireNonNull(complaint, "complaint"), now());
    }

    public SlaAssessment slaFor(Complaint complaint) {
        Objects.requireNonNull(complaint, "complaint");
        LocalDateTime resolvedAt = eventRepository.findByComplaintId(complaint.getId()).stream()
            .filter(event -> event.getEventType() == TicketEventType.TICKET_RESOLVED)
            .map(TicketEvent::getTimestamp)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        return slaService.assess(complaint, resolvedAt);
    }

    public List<Complaint> visibleTo(User user) {
        requireUser(user);
        return repository.findAll().stream()
            .filter(complaint -> canView(user, complaint))
            .sorted(Comparator.comparing(this::priorityFor).reversed().thenComparing(Complaint::getCreatedAt))
            .toList();
    }

    public List<Complaint> all() {
        return repository.findAll();
    }

    public Complaint get(User actor, int complaintId) {
        Complaint complaint = requireTicket(complaintId);
        requireCanView(actor, complaint);
        return complaint.copy();
    }

    public List<TicketEvent> eventsFor(User actor, int complaintId) {
        Complaint complaint = requireTicket(complaintId);
        requireCanView(actor, complaint);
        return eventRepository.findByComplaintId(complaintId);
    }

    public Complaint create(User user, String title, String description, String location,
                            Category category, Severity severity) {
        requireUser(user);
        if (user.getRole() != Role.STUDENT) {
            throw new ValidationException("Only students can register complaints.");
        }
        if (title == null || title.trim().length() < 5 || title.trim().length() > 90) {
            throw new ValidationException("Title must contain 5-90 characters.");
        }
        if (description == null || description.trim().length() < 10 || description.trim().length() > 1000) {
            throw new ValidationException("Description must contain 10-1000 characters.");
        }
        if (location == null || location.trim().length() < 3 || location.trim().length() > 100) {
            throw new ValidationException("Location must contain 3-100 characters.");
        }
        if (category == null || severity == null) {
            throw new ValidationException("Select a category and severity.");
        }
        LocalDateTime timestamp = now();
        Complaint complaint = new Complaint(
            repository.nextId(), user.getId(), title.trim(), description.trim(), location.trim(),
            category, severity, timestamp, Status.OPEN, null, timestamp);
        repository.save(complaint);
        recordAfterComplaintSave(complaint, user, TicketEventType.TICKET_CREATED, null);
        notifyListeners(complaint, "CREATED");
        return complaint;
    }

    public void assign(User actor, int ticketId, Technician technician) {
        requireUser(actor);
        if (actor.getRole() != Role.ADMIN) {
            throw new ValidationException("Only administrators may assign complaints.");
        }
        if (technician == null) throw new ValidationException("Select a technician.");
        Complaint complaint = requireTicket(ticketId);
        if (complaint.getStatus() == Status.RESOLVED) {
            throw new ValidationException("Resolved complaints cannot be reassigned.");
        }
        String previousTechnicianId = complaint.getTechnicianId();
        boolean reassignment = previousTechnicianId != null;
        LocalDateTime timestamp = now();
        complaint.assignTo(technician.getId(), timestamp);
        repository.save(complaint);
        recordAfterComplaintSave(complaint, actor,
            reassignment ? TicketEventType.TECHNICIAN_REASSIGNED : TicketEventType.TECHNICIAN_ASSIGNED,
            reassignment ? previousTechnicianId + "|" + technician.getId() : technician.getId());
        notifyListeners(complaint, reassignment ? "REASSIGNED" : "ASSIGNED");
    }

    public void changeStatus(User actor, int ticketId, Status target) {
        changeStatus(actor, ticketId, target, null);
    }

    public void changeStatus(User actor, int ticketId, Status target, String resolutionNote) {
        requireUser(actor);
        if (target == null) throw new ValidationException("Select a target status.");
        Complaint complaint = requireTicket(ticketId);
        if (actor.getRole() == Role.STUDENT) {
            throw new ValidationException("Students cannot update complaint status.");
        }
        if (actor.getRole() == Role.TECHNICIAN && !actor.getId().equals(complaint.getTechnicianId())) {
            throw new ValidationException("You can update only tickets assigned to you.");
        }
        Status current = complaint.getStatus();
        boolean allowed = switch (current) {
            case OPEN -> false;
            case ASSIGNED -> target == Status.IN_PROGRESS;
            case IN_PROGRESS -> target == Status.RESOLVED;
            case RESOLVED -> false;
        };
        if (!allowed) {
            throw new ValidationException("Invalid transition: " + current + " -> " + target
                + ". Follow ASSIGNED -> IN_PROGRESS -> RESOLVED.");
        }
        String note = null;
        TicketEventType eventType = TicketEventType.STATUS_CHANGED;
        if (target == Status.RESOLVED) {
            note = validatedNote(resolutionNote, 10, "Resolution note");
            eventType = TicketEventType.TICKET_RESOLVED;
        } else {
            note = current.name() + "|" + target.name();
        }
        complaint.moveTo(target, now());
        repository.save(complaint);
        recordAfterComplaintSave(complaint, actor, eventType, note);
        notifyListeners(complaint, "STATUS_UPDATED");
    }

    public TicketEvent addComment(User actor, int ticketId, String comment) {
        requireUser(actor);
        Complaint complaint = requireTicket(ticketId);
        requireCanView(actor, complaint);
        String note = validatedNote(comment, 2, "Comment");
        TicketEvent event = appendEvent(complaint.getId(), actor, TicketEventType.COMMENT_ADDED, note);
        notifyListeners(complaint, "COMMENT_ADDED");
        return event;
    }

    public TicketEvent addProgressNote(User actor, int ticketId, String progressNote) {
        requireUser(actor);
        Complaint complaint = requireTicket(ticketId);
        if (actor.getRole() != Role.TECHNICIAN) {
            throw new ValidationException("Only technicians may add progress notes.");
        }
        if (!actor.getId().equals(complaint.getTechnicianId())) {
            throw new ValidationException("You can add progress notes only to tickets assigned to you.");
        }
        if (complaint.getStatus() == Status.RESOLVED) {
            throw new ValidationException("Resolved complaints cannot receive progress notes.");
        }
        String note = validatedNote(progressNote, 5, "Progress note");
        TicketEvent event = appendEvent(complaint.getId(), actor, TicketEventType.PROGRESS_UPDATED, note);
        notifyListeners(complaint, "PROGRESS_UPDATED");
        return event;
    }

    public Technician recommendTechnician() {
        Map<String, Long> workload = repository.findAll().stream()
            .filter(complaint -> complaint.getStatus() != Status.RESOLVED && complaint.getTechnicianId() != null)
            .collect(Collectors.groupingBy(Complaint::getTechnicianId, Collectors.counting()));
        return directory.technicians().stream()
            .min(Comparator.comparingLong(technician -> workload.getOrDefault(technician.getId(), 0L)))
            .orElse(null);
    }

    public List<TechnicianRecommendation> recommendTechnicians(User actor, int ticketId) {
        requireUser(actor);
        if (actor.getRole() != Role.ADMIN) {
            throw new ValidationException("Only administrators may request technician recommendations.");
        }
        Complaint complaint = requireTicket(ticketId);
        if (complaint.getStatus() == Status.RESOLVED) {
            throw new ValidationException("Resolved complaints do not need technician recommendations.");
        }
        return recommendationService.recommendationsFor(complaint);
    }

    public long overdueCount(User actor) {
        requireUser(actor);
        return visibleTo(actor).stream().map(this::slaFor)
            .filter(assessment -> assessment.status() == SlaStatus.OVERDUE).count();
    }

    public List<Complaint> urgentQueue() {
        PriorityQueue<Complaint> queue = new PriorityQueue<>(
            Comparator.comparingInt(this::score).reversed().thenComparing(Complaint::getCreatedAt));
        repository.findAll().stream()
            .filter(complaint -> complaint.getStatus() != Status.RESOLVED)
            .forEach(queue::add);
        List<Complaint> result = new ArrayList<>();
        while (!queue.isEmpty()) result.add(queue.remove());
        return result;
    }

    private Complaint requireTicket(int id) {
        return repository.findAll().stream()
            .filter(complaint -> complaint.getId() == id)
            .findFirst()
            .orElseThrow(() -> new ValidationException("Complaint #" + id + " not found."));
    }

    private void requireCanView(User actor, Complaint complaint) {
        requireUser(actor);
        if (!canView(actor, complaint)) {
            throw new ValidationException("You are not authorized to view or update this complaint.");
        }
    }

    private boolean canView(User user, Complaint complaint) {
        return switch (user.getRole()) {
            case ADMIN -> true;
            case STUDENT -> complaint.getStudentId().equals(user.getId());
            case TECHNICIAN -> user.getId().equals(complaint.getTechnicianId());
        };
    }

    private void requireUser(User user) {
        if (user == null) throw new ValidationException("A signed-in user is required.");
    }

    private String validatedNote(String value, int minimumLength, String label) {
        if (value == null || value.trim().length() < minimumLength || value.trim().length() > 1000) {
            throw new ValidationException(label + " must contain " + minimumLength + "-1000 characters.");
        }
        return value.trim();
    }

    private synchronized TicketEvent appendEvent(int complaintId, User actor, TicketEventType type, String note) {
        TicketEvent event = new TicketEvent(
            eventRepository.nextId(), complaintId, actor.getId(), type, now(), note);
        eventRepository.save(event);
        return event;
    }

    private void recordAfterComplaintSave(Complaint complaint, User actor, TicketEventType type, String note) {
        try {
            appendEvent(complaint.getId(), actor, type, note);
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Complaint #" + complaint.getId()
                + " was saved, but its activity event could not be recorded. " + exception.getMessage(), exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void notifyListeners(Complaint complaint, String action) {
        for (TicketListener listener : listeners) listener.onTicketChanged(complaint.copy(), action);
    }
}
