package com.conferenceCenter.model;

/**
 * Abstract intermediate class representing a conference-center employee.
 * Extends Person (firstName + lastName already stored atomically there).
 * Adds employee-specific fields: ID, years of experience, date of birth, and gender.
 * Inheritance chain: Admin / AssignedEmployee → Employee → Person
 */
public abstract class Employee extends Person {

    private int    employeeId;
    private int    yearsOfExperience;
    private String dateOfBirth;   // ISO-8601: YYYY-MM-DD
    private String gender;        // 'Male' | 'Female'

    public Employee() {}

    public Employee(int employeeId, String firstName, String lastName, String phone,
                    int yearsOfExperience, String dateOfBirth, String gender) {
        super(firstName, lastName, phone);
        this.employeeId        = employeeId;
        this.yearsOfExperience = yearsOfExperience;
        this.dateOfBirth       = dateOfBirth;
        this.gender            = gender;
    }

    public int    getEmployeeId()               { return employeeId; }
    public void   setEmployeeId(int id)         { this.employeeId = id; }
    public int    getYearsOfExperience()        { return yearsOfExperience; }
    public void   setYearsOfExperience(int yoe) { this.yearsOfExperience = yoe; }
    public String getDateOfBirth()              { return dateOfBirth; }
    public void   setDateOfBirth(String dob)    { this.dateOfBirth = dob; }
    public String getGender()                   { return gender; }
    public void   setGender(String g)           { this.gender = g; }
}
