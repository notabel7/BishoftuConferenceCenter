package com.conferenceCenter.dao;

import com.conferenceCenter.model.Admin;
import java.sql.*;

public class AdminDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Returns the Admin if credentials match, otherwise null. */
    public Admin authenticate(String username, String password) throws SQLException {
        String sql =
            "SELECT admin_id, first_name, last_name, phone, " +
            "       years_of_experience, date_of_birth, gender, username, password " +
            "FROM admins WHERE username=? AND password=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Admin(
                        rs.getInt("admin_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getInt("years_of_experience"),
                        rs.getString("date_of_birth"),
                        rs.getString("gender"),
                        rs.getString("username"),
                        rs.getString("password")
                    );
                }
            }
        }
        return null;
    }

    public boolean updatePassword(int adminId, String newPassword) throws SQLException {
        String sql = "UPDATE admins SET password=? WHERE admin_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setInt(2, adminId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateProfile(Admin admin) throws SQLException {
        String sql =
            "UPDATE admins SET first_name=?, last_name=?, phone=?, " +
            "                  years_of_experience=?, date_of_birth=? " +
            "WHERE admin_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, admin.getFirstName());
            ps.setString(2, admin.getLastName());
            ps.setString(3, admin.getPhone());
            ps.setInt(4, admin.getYearsOfExperience());
            ps.setString(5, admin.getDateOfBirth());
            ps.setInt(6, admin.getEmployeeId());
            return ps.executeUpdate() > 0;
        }
    }
}
