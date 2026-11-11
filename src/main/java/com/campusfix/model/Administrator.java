package com.campusfix.model;
public final class Administrator extends User {
    public Administrator(String id,String name,String username){super(id,name,username);}
    @Override public Role getRole(){return Role.ADMIN;}
}
