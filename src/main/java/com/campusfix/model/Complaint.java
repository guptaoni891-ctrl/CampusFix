package com.campusfix.model;
import java.time.LocalDateTime;
import java.util.Objects;
public final class Complaint {
    private final int id;
    private final String studentId, title, description, location;
    private final Category category;
    private final Severity severity;
    private final LocalDateTime createdAt;
    private Status status;
    private String technicianId;
    private LocalDateTime updatedAt;
    public Complaint(int id,String studentId,String title,String description,String location, Category category,Severity severity,LocalDateTime createdAt,Status status,String technicianId,LocalDateTime updatedAt) {
        this.id=id;this.studentId=Objects.requireNonNull(studentId);this.title=Objects.requireNonNull(title);
        this.description=Objects.requireNonNull(description);this.location=Objects.requireNonNull(location);
        this.category=Objects.requireNonNull(category);this.severity=Objects.requireNonNull(severity);
        this.createdAt=Objects.requireNonNull(createdAt);this.status=Objects.requireNonNull(status);
        this.technicianId=technicianId;this.updatedAt=Objects.requireNonNull(updatedAt);
    }
    public int getId(){return id;}
    public String getStudentId(){return studentId;}
    public String getTitle(){return title;}
    public String getDescription(){return description;}
    public String getLocation(){return location;}
    public Category getCategory(){return category;}
    public Severity getSeverity(){return severity;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public Status getStatus(){return status;}
    public String getTechnicianId(){return technicianId;}
    public void assignTo(String technicianId){this.technicianId=Objects.requireNonNull(technicianId);this.status=Status.ASSIGNED;this.updatedAt=LocalDateTime.now();}
    public void moveTo(Status status){this.status=Objects.requireNonNull(status);this.updatedAt=LocalDateTime.now();}
    public Complaint copy(){return new Complaint(id,studentId,title,description,location,category,severity,createdAt,status,technicianId,updatedAt);}
}
