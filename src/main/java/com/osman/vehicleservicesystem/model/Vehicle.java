package com.osman.vehicleservicesystem.model;

import java.sql.Date;

public class Vehicle {
    private long vehicleId;
    private String plateNo;
    private String vinNo;
    private String make;
    private String model;
    private int year;
    private String colour;
    private long currentKm;
    private String status;
    private String notes;
    private Date serviceEntryDate;

    public Vehicle() {}

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }

    public String getPlateNo() { return plateNo; }
    public void setPlateNo(String plateNo) { this.plateNo = plateNo; }

    public String getVinNo() { return vinNo; }
    public void setVinNo(String vinNo) { this.vinNo = vinNo; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getColour() { return colour; }
    public void setColour(String colour) { this.colour = colour; }

    public long getCurrentKm() { return currentKm; }
    public void setCurrentKm(long currentKm) { this.currentKm = currentKm; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Date getServiceEntryDate() { return serviceEntryDate; }
    public void setServiceEntryDate(Date serviceEntryDate) { this.serviceEntryDate = serviceEntryDate; }
}
