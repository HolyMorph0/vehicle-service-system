package com.osman.vehicleservicesystem;

import com.osman.vehicleservicesystem.dao.AssignmentDAO;
import com.osman.vehicleservicesystem.model.Assignment;

import java.sql.Timestamp;

public class AssignmentSPTest {
    public static void main(String[] args) throws Exception {
        AssignmentDAO dao = new AssignmentDAO();

        System.out.println("=== ACTIVE BEFORE ===");
        for (Assignment a : dao.findActive()) {
            System.out.println(a.getAssignmentId() + " | D=" + a.getDriverId() + " V=" + a.getVehicleId() +
                    " | start=" + a.getStartDatetime() + " | end=" + a.getEndDatetime());
        }

        Timestamp start = Timestamp.valueOf("2025-12-23 11:00:00");
        dao.createAssignmentSP(2, 2, 185000, start);
        System.out.println("Created new assignment via SP.");

        System.out.println("=== ACTIVE AFTER CREATE ===");
        Assignment newestActive = null;
        for (Assignment a : dao.findActive()) {
            System.out.println(a.getAssignmentId() + " | D=" + a.getDriverId() + " V=" + a.getVehicleId() +
                    " | start=" + a.getStartDatetime() + " | end=" + a.getEndDatetime());
            if (newestActive == null) newestActive = a;
        }

        if (newestActive != null) {
            Timestamp end = Timestamp.valueOf("2025-12-23 18:00:00");
            dao.closeAssignmentSP(newestActive.getAssignmentId(), newestActive.getStartKm() + 120, end);
            System.out.println("Closed assignment_id=" + newestActive.getAssignmentId() + " via SP.");
        }

        System.out.println("=== ACTIVE AFTER CLOSE ===");
        for (Assignment a : dao.findActive()) {
            System.out.println(a.getAssignmentId() + " | D=" + a.getDriverId() + " V=" + a.getVehicleId() +
                    " | start=" + a.getStartDatetime() + " | end=" + a.getEndDatetime());
        }
    }
}
