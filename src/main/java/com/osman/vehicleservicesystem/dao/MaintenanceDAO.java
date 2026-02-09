package com.osman.vehicleservicesystem.dao;

import com.osman.vehicleservice.db.DBConnection;
import com.osman.vehicleservicesystem.model.Maintenance;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaintenanceDAO{

    public List<Maintenance> findAll() throws SQLException {
        String sql = "SELECT maint_id, maint_date, maint_type, odometer_km, description, cost, vehicle_id " +
                     "FROM maintenance ORDER BY maint_id";

        List<Maintenance> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public List<Maintenance> findByVehicleId(long vehicleId) throws SQLException {
        String sql = "SELECT maint_id, maint_date, maint_type, odometer_km, description, cost, vehicle_id " +
                     "FROM maintenance WHERE vehicle_id=? " +
                     "ORDER BY maint_date DESC, maint_id DESC";

        List<Maintenance> list = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public long insert(Maintenance m) throws SQLException {
        String sql = "INSERT INTO maintenance (maint_date, maint_type, odometer_km, description, cost, vehicle_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setDate(1, m.getMaintDate());
            ps.setString(2, m.getMaintType());
            ps.setLong(3, m.getOdometerKm());      // long kullan: int/long kavgası biter
            ps.setString(4, m.getDescription());
            ps.setDouble(5, m.getCost());
            ps.setLong(6, m.getVehicleId());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1L;
            }
        }
    }

    public boolean update(Maintenance m) throws SQLException {
        String sql = "UPDATE maintenance " +
                     "SET maint_date=?, maint_type=?, odometer_km=?, description=?, cost=?, vehicle_id=? " +
                     "WHERE maint_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, m.getMaintDate());
            ps.setString(2, m.getMaintType());
            ps.setLong(3, m.getOdometerKm());
            ps.setString(4, m.getDescription());
            ps.setDouble(5, m.getCost());
            ps.setLong(6, m.getVehicleId());
            ps.setLong(7, m.getMaintId());

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(long maintId) throws SQLException {
        String sql = "DELETE FROM maintenance WHERE maint_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, maintId);
            return ps.executeUpdate() == 1;
        }
    }

    private Maintenance map(ResultSet rs) throws SQLException {
        Maintenance m = new Maintenance();
        m.setMaintId(rs.getLong("maint_id"));
        m.setMaintDate(rs.getDate("maint_date"));
        m.setMaintType(rs.getString("maint_type"));
        m.setOdometerKm(rs.getLong("odometer_km"));
        m.setDescription(rs.getString("description"));
        m.setCost(rs.getDouble("cost"));
        m.setVehicleId(rs.getLong("vehicle_id"));
        return m;
    }
}
