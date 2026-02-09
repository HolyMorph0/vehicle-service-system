package com.osman.vehicleservicesystem.model;

public class Driver {
    private long driverId;
    private String firstName;
    private String lastName;
    private String licenseNo;
    private String phone;

    public Driver() {}

    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getLicenseNo() { return licenseNo; }
    public void setLicenseNo(String licenseNo) { this.licenseNo = licenseNo; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
