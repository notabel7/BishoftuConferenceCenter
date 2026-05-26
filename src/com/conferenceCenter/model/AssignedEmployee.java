package com.conferenceCenter.model;

/**
 * An employee assigned to a specific event (max 3 per event).
 * Inheritance: AssignedEmployee -> Employee -> Person
 */
public class AssignedEmployee extends Employee {

    private int    eventId;
    private String eventName;   // convenience field — not persisted separately

    public AssignedEmployee() {}

    /** Constructor used when loading employees not yet tied to an event. */
    public AssignedEmployee(int employeeId, String firstName, String lastName, String phone,
                            int yearsOfExperience, String dateOfBirth, String gender) {
        super(employeeId, firstName, lastName, phone, yearsOfExperience, dateOfBirth, gender);
    }

    /** Constructor used when loading employees with their event assignment. */
    public AssignedEmployee(int employeeId, String firstName, String lastName, String phone,
                            int yearsOfExperience, String dateOfBirth, String gender, int eventId) {
        super(employeeId, firstName, lastName, phone, yearsOfExperience, dateOfBirth, gender);
        this.eventId = eventId;
    }

    public int    getEventId()              { return eventId; }
    public void   setEventId(int id)        { this.eventId = id; }
    public String getEventName()            { return eventName; }
    public void   setEventName(String name) { this.eventName = name; }

    @Override
    public String getRole() { return "Event Employee"; }
}
