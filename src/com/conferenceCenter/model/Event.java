package com.conferenceCenter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conference or event booking.
 * Owner name is stored atomically as first_name + last_name (1NF).
 * An event may span multiple halls and has up to 3 assigned employees.
 */
public class Event {

    private int    eventId;
    private String name;
    private String type;
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerPhone;

    private List<Hall>             halls     = new ArrayList<>();
    private List<AssignedEmployee> employees = new ArrayList<>();

    public Event() {}

    public Event(int eventId, String name, String type,
                 String ownerFirstName, String ownerLastName, String ownerPhone) {
        this.eventId        = eventId;
        this.name           = name;
        this.type           = type;
        this.ownerFirstName = ownerFirstName;
        this.ownerLastName  = ownerLastName;
        this.ownerPhone     = ownerPhone;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int    getEventId()                          { return eventId; }
    public void   setEventId(int id)                    { this.eventId = id; }
    public String getName()                             { return name; }
    public void   setName(String name)                  { this.name = name; }
    public String getType()                             { return type; }
    public void   setType(String type)                  { this.type = type; }
    public String getOwnerFirstName()                   { return ownerFirstName; }
    public void   setOwnerFirstName(String fn)          { this.ownerFirstName = fn; }
    public String getOwnerLastName()                    { return ownerLastName; }
    public void   setOwnerLastName(String ln)           { this.ownerLastName = ln; }

    /** Convenience display method — never stored as a single field in the DB. */
    public String getOwnerFullName()                    { return ownerFirstName + " " + ownerLastName; }

    public String getOwnerPhone()                       { return ownerPhone; }
    public void   setOwnerPhone(String p)               { this.ownerPhone = p; }
    public List<Hall>             getHalls()            { return halls; }
    public void   setHalls(List<Hall> halls)            { this.halls = halls; }
    public List<AssignedEmployee> getEmployees()        { return employees; }
    public void   setEmployees(List<AssignedEmployee> e){ this.employees = e; }

    public void addHall(Hall hall)              { halls.add(hall); }
    public void addEmployee(AssignedEmployee e)  { employees.add(e); }

    /** Convenience: comma-separated hall names for table display. */
    public String getHallNames() {
        StringBuilder sb = new StringBuilder();
        for (Hall h : halls) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(h.getName());
        }
        return sb.toString();
    }

    @Override
    public String toString() { return name + " [" + type + "]"; }
}
