package com.osman.vehicleservicesystem.model;

import java.sql.Timestamp;

public class Assignment {
    private long assignmentId;
    private long startKm;
    private Long endKm;
    private Timestamp startDatetime;
    private Timestamp endDatetime;
    private long driverId;
    private long vehicleId;

    public Assignment() {}

    public long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(long assignmentId) { this.assignmentId = assignmentId; }

    public long getStartKm() { return startKm; }
    public void setStartKm(long startKm) { this.startKm = startKm; }

    public Long getEndKm() { return endKm; }
    public void setEndKm(Long endKm) { this.endKm = endKm; }

    public Timestamp getStartDatetime() { return startDatetime; }
    public void setStartDatetime(Timestamp startDatetime) { this.startDatetime = startDatetime; }

    public Timestamp getEndDatetime() { return endDatetime; }
    public void setEndDatetime(Timestamp endDatetime) { this.endDatetime = endDatetime; }

    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }
}
