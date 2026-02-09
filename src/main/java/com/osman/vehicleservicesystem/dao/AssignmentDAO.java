package com.osman.vehicleservicesystem.dao;

import com.osman.vehicleservice.db.DBConnection;
import com.osman.vehicleservicesystem.model.Assignment;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO {

    public void createAssignmentSP(long driverId, long vehicleId, long startKm, Timestamp startDatetime) throws SQLException {
        String sql = "{CALL sp_create_assignment(?, ?, ?, ?)}";

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setLong(1, driverId);
            cs.setLong(2, vehicleId);
            cs.setLong(3, startKm);
            cs.setTimestamp(4, startDatetime);
            cs.execute();
        }
    }

    public void closeAssignmentSP(long assignmentId, long endKm, Timestamp endDatetime) throws SQLException {
        String sql = "{CALL sp_close_assignment(?, ?, ?)}";

        try (Connection con = DBConnection.getConnection();
             CallableStatement cs = con.prepareCall(sql)) {

            cs.setLong(1, assignmentId);
            cs.setLong(2, endKm);
            cs.setTimestamp(3, endDatetime);
            cs.execute();
        }
    }

    public List<Assignment> findAll() throws SQLException {
        String sql =
                "SELECT assignment_id, start_km, end_km, start_datetime, end_datetime, driver_id, vehicle_id " +
                "FROM assignment ORDER BY assignment_id";

        List<Assignment> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }

        return list;
    }

    public List<Assignment> findActive() throws SQLException {
        String sql =
                "SELECT assignment_id, start_km, end_km, start_datetime, end_datetime, driver_id, vehicle_id " +
                "FROM assignment WHERE end_datetime IS NULL ORDER BY start_datetime DESC";

        List<Assignment> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }

        return list;
    }

    public Assignment findById(long id) throws SQLException {
        String sql =
                "SELECT assignment_id, start_km, end_km, start_datetime, end_datetime, driver_id, vehicle_id " +
                "FROM assignment WHERE assignment_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Assignment map(ResultSet rs) throws SQLException {
        Assignment a = new Assignment();
        a.setAssignmentId(rs.getLong("assignment_id"));
        a.setStartKm(rs.getLong("start_km"));

        long endKmVal = rs.getLong("end_km");
        if (rs.wasNull()) a.setEndKm(null);
        else a.setEndKm(endKmVal);

        a.setStartDatetime(rs.getTimestamp("start_datetime"));
        a.setEndDatetime(rs.getTimestamp("end_datetime"));
        a.setDriverId(rs.getLong("driver_id"));
        a.setVehicleId(rs.getLong("vehicle_id"));
        return a;
    }
}
