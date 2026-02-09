    package com.osman.vehicleservicesystem.dao;
    
import com.osman.vehicleservice.db.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    public List<String> activeAssignmentsDetailed() throws SQLException {
        String sql = "SELECT * FROM vw_active_assignments_detailed ORDER BY start_datetime DESC";
        List<String> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(
                    "ASSIGN#" + rs.getLong("assignment_id") +
                    " | " + rs.getTimestamp("start_datetime") +
                    " | DRIVER=" + rs.getString("driver_name") + " (" + rs.getString("license_no") + ")" +
                    " | VEHICLE=" + rs.getString("plate_no") + " " + rs.getString("make") + " " + rs.getString("model")
                );
            }
        }
        return out;
    }

    public List<String> maintenanceHistoryByVehicle(long vehicleId) throws SQLException {
        String sql =
            "SELECT m.maint_id, m.maint_date, m.maint_type, m.odometer_km, m.cost, v.plate_no " +
            "FROM maintenance m " +
            "JOIN vehicle v ON v.vehicle_id = m.vehicle_id " +
            "WHERE m.vehicle_id = ? " +
            "ORDER BY m.maint_date DESC";

        List<String> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(
                        "MAINT#" + rs.getLong("maint_id") +
                        " | " + rs.getDate("maint_date") +
                        " | " + rs.getString("maint_type") +
                        " | Cost=" + String.format("%.2f", rs.getDouble("cost"))
                    );
                }
            }
        }
        return out;
    }

    public List<String> totalMaintenanceCostPerVehicle() throws SQLException {
        String sql = "SELECT * FROM vw_vehicle_maintenance_summary ORDER BY total_cost DESC";
        List<String> out = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(
                    "VEHICLE#" + rs.getLong("vehicle_id") +
                    " | " + rs.getString("plate_no") +
                    " | Count=" + rs.getLong("maint_count") +
                    " | TOTAL COST=" + String.format("%.2f", rs.getDouble("total_cost"))
                );
            }
        }
        return out;
    }
}
