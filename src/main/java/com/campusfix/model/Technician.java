package com.campusfix.model;
public final class Technician extends User {
    private final String specialty;
    public Technician(String id,String name,String username,String specialty){super(id,name,username);this.specialty=specialty;}
    public String getSpecialty(){return specialty;}
    @Override public Role getRole(){return Role.TECHNICIAN;}
    @Override public String toString(){return getName()+" - "+specialty;}
}
