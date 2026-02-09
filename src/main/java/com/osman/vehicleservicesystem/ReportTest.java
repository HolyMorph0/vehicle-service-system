package com.osman.vehicleservicesystem;

import com.osman.vehicleservicesystem.dao.ReportDAO;

public class ReportTest {
    public static void main(String[] args) throws Exception {
        ReportDAO r = new ReportDAO();

        System.out.println("=== ACTIVE ASSIGNMENTS (DETAILED) ===");
        r.activeAssignmentsDetailed().forEach(System.out::println);

        System.out.println("=== MAINTENANCE HISTORY: VEHICLE 1 ===");
        r.maintenanceHistoryByVehicle(1).forEach(System.out::println);

        System.out.println("=== TOTAL MAINTENANCE COST PER VEHICLE ===");
        r.totalMaintenanceCostPerVehicle().forEach(System.out::println);
    }
}
