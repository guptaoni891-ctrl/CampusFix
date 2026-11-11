package com.campusfix.model;
public final class Student extends User {
    public Student(String id,String name,String username){super(id,name,username);}
    @Override public Role getRole(){return Role.STUDENT;}
}
