package com.osman.vehicleservicesystem;

import com.osman.vehicleservicesystem.dao.MaintenanceDAO;
import com.osman.vehicleservicesystem.model.Maintenance;
import java.sql.Date;

public class MaintenanceCRUDTest {

    public static void main(String[] args) {
        try {
            // DB'li DAO (RAM yok)
            MaintenanceDAO dao = new MaintenanceDAO();

            System.out.println("=== LIST ALL MAINTENANCE (BAŞLANGIÇ) ===");
            for (Maintenance m : dao.findAll()) {
                System.out.println(m.getMaintId() + " | V=" + m.getVehicleId() + " | " + m.getMaintType());
            }

            // Yeni kayıt
            Maintenance nm = new Maintenance();
            nm.setMaintDate(Date.valueOf("2025-12-23"));
            nm.setMaintType("General Check");
            nm.setOdometerKm(121500);
            nm.setDescription("Created from Java test");
            nm.setCost(1000.00);
            nm.setVehicleId(1);

            long newId = dao.insert(nm); // FIX: long -> int hatasını önler
            System.out.println("Inserted maint_id = " + newId);

            System.out.println("=== VEHICLE 1 MAINTENANCE (EKLEME SONRASI) ===");
            for (Maintenance m : dao.findByVehicleId(1)) {
                System.out.println(m.getMaintId() + " | " + m.getMaintDate() + " | " + m.getMaintType());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
