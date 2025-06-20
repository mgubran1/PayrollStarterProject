package com.company.payroll.loads;

import com.company.payroll.employees.Employee;
import java.time.LocalDate;

public class Load {
    public enum Status { BOOKED, IN_TRANSIT, DELIVERED, PAID, CANCELLED }

    private int id;
    private String loadNumber;
    private String customer;
    private String pickUpLocation;
    private String dropLocation;
    private Employee driver;
    private Status status;
    private double grossAmount;
    private String notes;
    private LocalDate deliveryDate; // <-- NEW FIELD

    // Optional: Attachments, Created/Modified date, etc.

    // UPDATED CONSTRUCTOR
    public Load(int id, String loadNumber, String customer, String pickUpLocation, String dropLocation,
                Employee driver, Status status, double grossAmount, String notes, LocalDate deliveryDate) {
        this.id = id;
        this.loadNumber = loadNumber;
        this.customer = customer;
        this.pickUpLocation = pickUpLocation;
        this.dropLocation = dropLocation;
        this.driver = driver;
        this.status = status;
        this.grossAmount = grossAmount;
        this.notes = notes;
        this.deliveryDate = deliveryDate;
    }

    // Backwards compatible constructor for legacy code
    public Load(int id, String loadNumber, String customer, String pickUpLocation, String dropLocation,
                Employee driver, Status status, double grossAmount, String notes) {
        this(id, loadNumber, customer, pickUpLocation, dropLocation, driver, status, grossAmount, notes, null);
    }

    // --- Getters and setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLoadNumber() { return loadNumber; }
    public void setLoadNumber(String loadNumber) { this.loadNumber = loadNumber; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getPickUpLocation() { return pickUpLocation; }
    public void setPickUpLocation(String pickUpLocation) { this.pickUpLocation = pickUpLocation; }

    public String getDropLocation() { return dropLocation; }
    public void setDropLocation(String dropLocation) { this.dropLocation = dropLocation; }

    public Employee getDriver() { return driver; }
    public void setDriver(Employee driver) { this.driver = driver; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(LocalDate deliveryDate) { this.deliveryDate = deliveryDate; }

    @Override
    public String toString() {
        return loadNumber + " - " + customer + " (" + status + ")";
    }
}