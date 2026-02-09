package com.osman.vehicleservicesystem.model;

import java.sql.Date;

public class Maintenance {
    private long maintId;
    private Date maintDate;
    private String maintType;
    private long odometerKm;
    private String description;
    private double cost;
    private long vehicleId;

    public Maintenance() {}

    public long getMaintId() { return maintId; }
    public void setMaintId(long maintId) { this.maintId = maintId; }

    public Date getMaintDate() { return maintDate; }
    public void setMaintDate(Date maintDate) { this.maintDate = maintDate; }

    public String getMaintType() { return maintType; }
    public void setMaintType(String maintType) { this.maintType = maintType; }

    public long getOdometerKm() { return odometerKm; }
    public void setOdometerKm(long odometerKm) { this.odometerKm = odometerKm; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }
}
