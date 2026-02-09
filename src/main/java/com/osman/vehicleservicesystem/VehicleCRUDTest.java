package com.osman.vehicleservicesystem;

import com.osman.vehicleservicesystem.dao.VehicleDAO;
import com.osman.vehicleservicesystem.model.Vehicle;

import java.sql.Date;

public class VehicleCRUDTest {
    public static void main(String[] args) throws Exception {
        VehicleDAO dao = new VehicleDAO();

        System.out.println("=== LIST ALL ===");
        dao.findAll().forEach(v ->
                System.out.println(v.getVehicleId() + " | " + v.getPlateNo() + " | " + v.getVinNo())
        );

        Vehicle nv = new Vehicle();
        nv.setPlateNo("07TEST777");
        nv.setVinNo("WBA00000000000123");
        nv.setMake("BMW");
        nv.setModel("318i");
        nv.setYear(2017);
        nv.setColour("Gray");
        nv.setCurrentKm(150000);
        nv.setStatus("ACTIVE");
        nv.setNotes("Inserted from DAO test");
        nv.setServiceEntryDate(Date.valueOf("2025-12-23"));

        long newId = dao.insert(nv);
        System.out.println("Inserted ID = " + newId);

        System.out.println("=== SEARCH '07' ===");
        dao.searchByPlateOrVin("07").forEach(v ->
                System.out.println(v.getVehicleId() + " | " + v.getPlateNo())
        );
    }
}
