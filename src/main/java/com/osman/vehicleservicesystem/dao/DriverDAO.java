package com.osman.vehicleservicesystem.dao;

import com.osman.vehicleservice.db.DBConnection;
import com.osman.vehicleservicesystem.model.Driver;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DriverDAO {

    public List<Driver> findAll() throws SQLException {
        String sql =
                "SELECT driver_id, first_name, last_name, license_no, phone " +
                "FROM drivers ORDER BY driver_id";

        List<Driver> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Driver findById(long id) throws SQLException {
        String sql =
                "SELECT driver_id, first_name, last_name, license_no, phone " +
                "FROM drivers WHERE driver_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Driver> searchByNameOrLicense(String term) throws SQLException {
        String sql =
                "SELECT driver_id, first_name, last_name, license_no, phone " +
                "FROM drivers " +
                "WHERE first_name LIKE ? OR last_name LIKE ? OR license_no LIKE ? " +
                "ORDER BY driver_id";

        List<Driver> list = new ArrayList<>();

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + term + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public long insert(Driver d) throws SQLException {
        String sql =
                "INSERT INTO drivers (first_name, last_name, license_no, phone) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, d.getFirstName());
            ps.setString(2, d.getLastName());
            ps.setString(3, d.getLicenseNo());
            ps.setString(4, d.getPhone());

            int affected = ps.executeUpdate();
            if (affected != 1) throw new SQLException("Driver insert başarısız (affectedRows=" + affected + ")");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    public boolean update(Driver d) throws SQLException {
        String sql =
                "UPDATE drivers SET first_name=?, last_name=?, license_no=?, phone=? " +
                "WHERE driver_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, d.getFirstName());
            ps.setString(2, d.getLastName());
            ps.setString(3, d.getLicenseNo());
            ps.setString(4, d.getPhone());
            ps.setLong(5, d.getDriverId());

            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM drivers WHERE driver_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Aktif assignment varsa (end_datetime IS NULL), o sürücünün kullandığı aracın plakasını döner.
     * Kaynak: vw_active_assignments_detailed (senin SQL'inde var)
     */
    public String findActiveVehicleByDriverId(long driverId) throws SQLException {
        String sql =
                "SELECT plate_no " +
                "FROM vw_active_assignments_detailed " +
                "WHERE driver_id = ? " +
                "LIMIT 1";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, driverId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("plate_no") : null;
            }
        }
    }

    private Driver map(ResultSet rs) throws SQLException {
        Driver d = new Driver();
        d.setDriverId(rs.getLong("driver_id"));
        d.setFirstName(rs.getString("first_name"));
        d.setLastName(rs.getString("last_name"));
        d.setLicenseNo(rs.getString("license_no"));
        d.setPhone(rs.getString("phone"));
        return d;
    }
}
