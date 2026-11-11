package com.campusfix;
import com.campusfix.model.*;
import com.campusfix.persistence.*;
import com.campusfix.service.*;
import com.campusfix.util.CsvExporter;
import java.nio.file.*;
import java.time.*;
public final class SmokeTest {
    private static void require(boolean pass,String message){if(!pass)throw new AssertionError(message);}
    private static void expectFailure(Runnable r){try{r.run();throw new AssertionError("Expected validation error");}catch(ValidationException expected){}}
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("campusfix-test-");
        try{
            FileTicketRepository repo=new FileTicketRepository(dir.resolve("test.tsv"));
            UserDirectory users=new UserDirectory();
            ComplaintService svc=new ComplaintService(repo,users,new WeightedPriorityScorer());
            User student=users.login("student","demo123"), admin=users.login("admin","demo123"), tech=users.login("tech","demo123");
            require(student instanceof Student,"Inheritance: expected Student");
            expectFailure(()->users.login("admin","bad"));
            expectFailure(()->svc.create(student,"x","too short","B",Category.ELECTRICAL,Severity.HIGH));
            Complaint first=svc.create(student,"Broken light fixture","Lights are not working in the study area.","Library 201",Category.ELECTRICAL,Severity.HIGH);
            require(first.getStatus()==Status.OPEN,"New tickets must start OPEN");
            require(svc.visibleTo(student).size()==1,"Student visibility");
            require(svc.visibleTo(tech).isEmpty(),"Unassigned tech visibility");
            expectFailure(()->svc.assign(student,first.getId(),users.technicians().get(0)));
            svc.assign(admin,first.getId(),users.technicians().get(0));
            require(svc.visibleTo(tech).size()==1,"Tech ticket visibility");
            expectFailure(()->svc.changeStatus(student,first.getId(),Status.IN_PROGRESS));
            expectFailure(()->svc.changeStatus(tech,first.getId(),Status.RESOLVED));
            svc.changeStatus(tech,first.getId(),Status.IN_PROGRESS);
            svc.changeStatus(tech,first.getId(),Status.RESOLVED);
            require(svc.visibleTo(student).get(0).getStatus()==Status.RESOLVED,"Resolved transition");
            require(new FileTicketRepository(dir.resolve("test.tsv")).findAll().size()==1,"Persistence after restart");
            Complaint low=new Complaint(88,"S01","Network issue","Slow WiFi","Hall A",Category.INTERNET,Severity.LOW,LocalDateTime.now(),Status.OPEN,null,LocalDateTime.now());
            Complaint high=new Complaint(89,"S01","Power outage","No power","Hall B",Category.ELECTRICAL,Severity.CRITICAL,LocalDateTime.now(),Status.OPEN,null,LocalDateTime.now());
            PriorityScorer scorer=new WeightedPriorityScorer();
            require(scorer.score(high,LocalDateTime.now())>scorer.score(low,LocalDateTime.now()),"Priority scoring");
            Path csv=dir.resolve("export.csv");CsvExporter.export(csv,svc.all(),scorer);
            require(Files.readString(csv).contains("Broken light fixture"),"CSV export");
            System.out.println("PASS: login, inheritance, validation, permissions, assignment, transitions, file persistence, priority scoring, CSV export");
        }finally{
            try(var stream=Files.walk(dir)){stream.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}
        }
    }
}
