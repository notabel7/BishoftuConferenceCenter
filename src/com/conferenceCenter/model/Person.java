package com.conferenceCenter.model;

/**
 * Abstract base class for all persons in the system.
 * Names are stored atomically as first_name and last_name (1NF).
 */
public abstract class Person {

    protected String firstName;
    protected String lastName;
    protected String phone;

    public Person() {}

    public Person(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName  = lastName;
        this.phone     = phone;
    }

    public String getFirstName()             { return firstName; }
    public void   setFirstName(String n)     { this.firstName = n; }
    public String getLastName()              { return lastName; }
    public void   setLastName(String n)      { this.lastName = n; }

    /** Convenience display method — never stored as a single field in the DB. */
    public String getFullName()              { return firstName + " " + lastName; }

    public String getPhone()                 { return phone; }
    public void   setPhone(String p)         { this.phone = p; }

    public abstract String getRole();

    @Override
    public String toString() { return getRole() + ": " + getFullName(); }
}
