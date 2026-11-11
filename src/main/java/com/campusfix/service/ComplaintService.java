package com.campusfix.service;
import com.campusfix.model.*;
import com.campusfix.persistence.TicketRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
public final class ComplaintService {
    private final TicketRepository repository;
    private final UserDirectory directory;
    private final PriorityScorer scorer;
    private final List<TicketListener> listeners=new ArrayList<>();
    public ComplaintService(TicketRepository repository,UserDirectory directory,PriorityScorer scorer){
        this.repository=repository;this.directory=directory;this.scorer=scorer;
    }
    public String persistenceMode(){return repository.mode();}
    public void addListener(TicketListener l){listeners.add(l);}
    private void notifyListeners(Complaint c,String action){for(TicketListener l:listeners)l.onTicketChanged(c.copy(),action);}
    public int score(Complaint c){return scorer.score(c,LocalDateTime.now());}
    public List<Complaint> visibleTo(User user){
        return repository.findAll().stream().filter(c->switch(user.getRole()){
            case ADMIN -> true;
            case STUDENT -> c.getStudentId().equals(user.getId());
            case TECHNICIAN -> user.getId().equals(c.getTechnicianId());
        }).sorted(Comparator.comparingInt(this::score).reversed().thenComparing(Complaint::getCreatedAt)).toList();
    }
    public List<Complaint> all(){return repository.findAll();}
    public Complaint create(User user,String title,String description,String location,Category category,Severity severity){
        if(user.getRole()!=Role.STUDENT)throw new ValidationException("Only students can register complaints.");
        if(title==null||title.trim().length()<5||title.length()>90)throw new ValidationException("Title must contain 5-90 characters.");
        if(description==null||description.trim().length()<10||description.length()>1000)throw new ValidationException("Description must contain 10-1000 characters.");
        if(location==null||location.trim().length()<3||location.length()>100)throw new ValidationException("Location must contain 3-100 characters.");
        if(category==null||severity==null)throw new ValidationException("Select a category and severity.");
        LocalDateTime now=LocalDateTime.now();
        Complaint c=new Complaint(repository.nextId(),user.getId(),title.trim(),description.trim(),location.trim(),category,severity,now,Status.OPEN,null,now);
        repository.save(c);notifyListeners(c,"CREATED");return c;
    }
    private Complaint requireTicket(int id){return repository.findAll().stream().filter(c->c.getId()==id).findFirst().orElseThrow(()->new ValidationException("Complaint #"+id+" not found."));}
    public void assign(User actor,int ticketId,Technician tech){
        if(actor.getRole()!=Role.ADMIN)throw new ValidationException("Only administrators may assign complaints.");
        if(tech==null)throw new ValidationException("Select a technician.");
        Complaint c=requireTicket(ticketId);
        if(c.getStatus()==Status.RESOLVED)throw new ValidationException("Resolved complaints cannot be reassigned.");
        c.assignTo(tech.getId());repository.save(c);notifyListeners(c,"ASSIGNED");
    }
    public void changeStatus(User actor,int ticketId,Status target){
        Complaint c=requireTicket(ticketId);
        if(actor.getRole()==Role.STUDENT)throw new ValidationException("Students cannot update complaint status.");
        if(actor.getRole()==Role.TECHNICIAN&&!actor.getId().equals(c.getTechnicianId()))throw new ValidationException("You can update only tickets assigned to you.");
        Status current=c.getStatus();
        boolean allowed=switch(current){
            case OPEN -> false;
            case ASSIGNED -> target==Status.IN_PROGRESS;
            case IN_PROGRESS -> target==Status.RESOLVED;
            case RESOLVED -> false;
        };
        if(!allowed)throw new ValidationException("Invalid transition: "+current+" -> "+target+". Follow ASSIGNED -> IN_PROGRESS -> RESOLVED.");
        c.moveTo(target);repository.save(c);notifyListeners(c,"STATUS_UPDATED");
    }
    public Technician recommendTechnician(){
        Map<String,Long> workload=repository.findAll().stream().filter(c->c.getStatus()!=Status.RESOLVED&&c.getTechnicianId()!=null)
            .collect(Collectors.groupingBy(Complaint::getTechnicianId,Collectors.counting()));
        return directory.technicians().stream().min(Comparator.comparingLong(t->workload.getOrDefault(t.getId(),0L))).orElse(null);
    }
    public List<Complaint> urgentQueue(){
        PriorityQueue<Complaint> queue=new PriorityQueue<>(Comparator.comparingInt(this::score).reversed().thenComparing(Complaint::getCreatedAt));
        repository.findAll().stream().filter(c->c.getStatus()!=Status.RESOLVED).forEach(queue::add);
        List<Complaint> result=new ArrayList<>();while(!queue.isEmpty())result.add(queue.remove());return result;
    }
}
