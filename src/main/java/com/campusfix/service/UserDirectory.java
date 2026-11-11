package com.campusfix.service;
import com.campusfix.model.*;
import java.util.*;
public final class UserDirectory {
    private final List<User> users=List.of(
        new Administrator("A01","Alex Morgan","admin"),
        new Student("S01","Riya Sharma","student"),
        new Student("S02","Omar Khan","student2"),
        new Technician("T01","Samir Patel","tech","Electrical / Internet"),
        new Technician("T02","Sara Ali","tech2","Plumbing / Facilities")
    );
    public User login(String username,String password){
        if(!"demo123".equals(password))throw new ValidationException("Invalid username or password.");
        return users.stream().filter(u->u.getUsername().equalsIgnoreCase(username.trim())).findFirst()
            .orElseThrow(()->new ValidationException("Invalid username or password."));
    }
    public User byId(String id){return users.stream().filter(u->u.getId().equals(id)).findFirst().orElse(null);}
    public List<Technician> technicians(){return users.stream().filter(u->u instanceof Technician).map(u->(Technician)u).toList();}
}
