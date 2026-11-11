package com.campusfix.service;
import com.campusfix.model.*;
/** Example scenarios are created once to make the first-run demo meaningful. */
public final class DemoSeeder {
    private DemoSeeder(){}
    public static void seedIfEmpty(ComplaintService s, UserDirectory users){
        if(!s.all().isEmpty())return;
        User a=users.login("admin","demo123"), student=users.login("student","demo123"), second=users.login("student2","demo123");
        Complaint a1=s.create(student,"WiFi disconnecting in lab","The computer lab network disconnects every few minutes.","Computer Lab 204",Category.INTERNET,Severity.HIGH);
        Complaint a2=s.create(student,"Power socket sparks","A power outlet sparks when plugging in a device.","Library 1F",Category.ELECTRICAL,Severity.CRITICAL);
        Complaint a3=s.create(second,"Water leak near staircase","Water is dripping onto the staircase and creating a slip hazard.","Block B, staircase",Category.PLUMBING,Severity.HIGH);
        Complaint a4=s.create(second,"Broken study chair","The study chair has a broken leg and is unsafe to use.","Reading Hall B",Category.FURNITURE,Severity.MEDIUM);
        Complaint a5=s.create(student,"Dirty cafeteria tables","Tables have not been cleaned after the lunch service.","Main Cafeteria",Category.CLEANING,Severity.LOW);
        Complaint a6=s.create(second,"Classroom projector faulty","The projector loses signal during lectures.","Lecture Hall 102",Category.ELECTRICAL,Severity.MEDIUM);
        s.assign(a,a1.getId(),users.technicians().get(0));
        s.changeStatus(a,a1.getId(),Status.IN_PROGRESS);
        s.assign(a,a3.getId(),users.technicians().get(1));
        s.assign(a,a6.getId(),users.technicians().get(0));
        s.changeStatus(a,a6.getId(),Status.IN_PROGRESS);
        s.changeStatus(a,a6.getId(),Status.RESOLVED);
    }
}
