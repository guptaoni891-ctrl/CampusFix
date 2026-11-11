package com.campusfix.model;
import java.util.Objects;
public abstract class User {
    private final String id, name, username;
    protected User(String id, String name, String username) {
        this.id=Objects.requireNonNull(id); this.name=Objects.requireNonNull(name); this.username=Objects.requireNonNull(username);
    }
    public final String getId(){return id;}
    public final String getName(){return name;}
    public final String getUsername(){return username;}
    public abstract Role getRole();
    public String getDisplayName(){return name + " (" + getRole() + ")";}
    @Override public String toString(){return name;}
}
