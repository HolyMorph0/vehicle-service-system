package com.osman.vehicleservicesystem;

import com.osman.vehicleservicesystem.dao.DriverDAO;
import com.osman.vehicleservicesystem.model.Driver;

public class DriverCRUDTest {
    public static void main(String[] args) throws Exception {
        DriverDAO dao = new DriverDAO();

        System.out.println("=== LIST ALL DRIVERS ===");
        dao.findAll().forEach(d ->
                System.out.println(d.getDriverId() + " | " + d.getFirstName() + " " + d.getLastName() + " | " + d.getLicenseNo())
        );

        Driver nd = new Driver();
        nd.setFirstName("Test");
        nd.setLastName("Driver");
        nd.setLicenseNo("TR-DR-9999");
        nd.setPhone("05550009999");

        long id = dao.insert(nd);
        System.out.println("Inserted driver_id=" + id);

        System.out.println("=== SEARCH 'TR-DR' ===");
        dao.searchByNameOrLicense("TR-DR").forEach(d ->
                System.out.println(d.getDriverId() + " | " + d.getLicenseNo())
        );
    }
}
