package com.osman.vehicleservicesystem.dao;

import com.osman.vehicleservice.db.DBConnection;
import com.osman.vehicleservicesystem.model.Vehicle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    public List<Vehicle> findAll() throws SQLException {
        String sql =
                "SELECT vehicle_id, plate_no, vin_no, make, model, model_year, colour, " +
                "current_km, status, notes, service_entry_date " +
                "FROM vehicle ORDER BY vehicle_id";

        List<Vehicle> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Vehicle findById(long id) throws SQLException {
        String sql =
                "SELECT vehicle_id, plate_no, vin_no, make, model, model_year, colour, " +
                "current_km, status, notes, service_entry_date " +
                "FROM vehicle WHERE vehicle_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Vehicle> searchByPlateOrVin(String term) throws SQLException {
        String sql =
                "SELECT vehicle_id, plate_no, vin_no, make, model, model_year, colour, " +
                "current_km, status, notes, service_entry_date " +
                "FROM vehicle " +
                "WHERE plate_no LIKE ? OR vin_no LIKE ? " +
                "ORDER BY vehicle_id";

        List<Vehicle> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + term + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public long insert(Vehicle v) throws SQLException {
        String sql =
                "INSERT INTO vehicle (plate_no, vin_no, make, model, model_year, colour, " +
                "current_km, status, notes, service_entry_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, v.getPlateNo());
            ps.setString(2, v.getVinNo());
            ps.setString(3, v.getMake());
            ps.setString(4, v.getModel());
            ps.setInt(5, v.getYear());
            ps.setString(6, v.getColour());
            ps.setLong(7, v.getCurrentKm());
            ps.setString(8, v.getStatus());
            ps.setString(9, v.getNotes());
            ps.setDate(10, v.getServiceEntryDate());

            int affected = ps.executeUpdate();
            if (affected != 1) throw new SQLException("Vehicle insert başarısız (affectedRows=" + affected + ")");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    public boolean update(Vehicle v) throws SQLException {
        String sql =
                "UPDATE vehicle SET plate_no=?, vin_no=?, make=?, model=?, model_year=?, colour=?, " +
                "current_km=?, status=?, notes=?, service_entry_date=? " +
                "WHERE vehicle_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, v.getPlateNo());
            ps.setString(2, v.getVinNo());
            ps.setString(3, v.getMake());
            ps.setString(4, v.getModel());
            ps.setInt(5, v.getYear());
            ps.setString(6, v.getColour());
            ps.setLong(7, v.getCurrentKm());
            ps.setString(8, v.getStatus());
            ps.setString(9, v.getNotes());
            ps.setDate(10, v.getServiceEntryDate());
            ps.setLong(11, v.getVehicleId());

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM vehicle WHERE vehicle_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Aktif assignment varsa (end_datetime IS NULL), o aracın sürücü adını döner.
     * Kaynak: vw_active_assignments_detailed (senin SQL'inde var)
     */
    public String findActiveDriverNameByVehicleId(long vehicleId) throws SQLException {
        String sql =
                "SELECT driver_name " +
                "FROM vw_active_assignments_detailed " +
                "WHERE vehicle_id = ? " +
                "LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, vehicleId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("driver_name") : null;
            }
        }
    }

    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getLong("vehicle_id"));
        v.setPlateNo(rs.getString("plate_no"));
        v.setVinNo(rs.getString("vin_no"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setYear(rs.getInt("model_year"));
        v.setColour(rs.getString("colour"));
        v.setCurrentKm(rs.getLong("current_km"));
        v.setStatus(rs.getString("status"));
        v.setNotes(rs.getString("notes"));
        v.setServiceEntryDate(rs.getDate("service_entry_date"));
        return v;
    }
}
