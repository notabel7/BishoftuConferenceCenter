package com.conferenceCenter.model;

/**
 * Admin is a privileged Employee who controls system access.
 * Inheritance: Admin -> Employee -> Person
 */
public class Admin extends Employee {

    private String username;
    private String password;

    public Admin() {}

    public Admin(int employeeId, String firstName, String lastName, String phone,
                 int yearsOfExperience, String dateOfBirth, String gender,
                 String username, String password) {
        super(employeeId, firstName, lastName, phone, yearsOfExperience, dateOfBirth, gender);
        this.username = username;
        this.password = password;
    }

    public String getUsername()              { return username; }
    public void   setUsername(String u)      { this.username = u; }
    public String getPassword()              { return password; }
    public void   setPassword(String p)      { this.password = p; }

    @Override
    public String getRole() { return "Admin"; }
}
