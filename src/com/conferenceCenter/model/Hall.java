package com.conferenceCenter.model;

/**
 * Represents one of the five conference halls available for rental.
 */
public class Hall {

    private int    hallId;
    private String name;
    private double pricePerDay;
    private int    capacity;       // number of seats

    public Hall() {}

    public Hall(int hallId, String name, double pricePerDay, int capacity) {
        this.hallId      = hallId;
        this.name        = name;
        this.pricePerDay = pricePerDay;
        this.capacity    = capacity;
    }

    public int    getHallId()                     { return hallId; }
    public void   setHallId(int id)               { this.hallId = id; }
    public String getName()                       { return name; }
    public void   setName(String name)            { this.name = name; }
    public double getPricePerDay()                { return pricePerDay; }
    public void   setPricePerDay(double price)    { this.pricePerDay = price; }
    public int    getCapacity()                   { return capacity; }
    public void   setCapacity(int capacity)       { this.capacity = capacity; }

    @Override
    public String toString() {
        return name + "  (Seats: " + capacity + ")";
    }
}
