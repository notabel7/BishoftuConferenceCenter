package com.conferenceCenter.dao;

import com.conferenceCenter.model.Hall;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HallDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Hall> getAllHalls() throws SQLException {
        List<Hall> list = new ArrayList<>();
        String sql = "SELECT hall_id, name, price_per_day, capacity FROM halls ORDER BY hall_id";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Hall(
                    rs.getInt("hall_id"),
                    rs.getString("name"),
                    rs.getDouble("price_per_day"),
                    rs.getInt("capacity")
                ));
            }
        }
        return list;
    }

    public Hall getHallById(int hallId) throws SQLException {
        String sql = "SELECT hall_id, name, price_per_day, capacity FROM halls WHERE hall_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, hallId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Hall(
                        rs.getInt("hall_id"),
                        rs.getString("name"),
                        rs.getDouble("price_per_day"),
                        rs.getInt("capacity")
                    );
                }
            }
        }
        return null;
    }

    public boolean addHall(Hall hall) throws SQLException {
        String sql = "INSERT INTO halls (name, price_per_day, capacity) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, hall.getName());
            ps.setDouble(2, hall.getPricePerDay());
            ps.setInt(3, hall.getCapacity());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateHall(Hall hall) throws SQLException {
        String sql = "UPDATE halls SET name=?, price_per_day=?, capacity=? WHERE hall_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, hall.getName());
            ps.setDouble(2, hall.getPricePerDay());
            ps.setInt(3, hall.getCapacity());
            ps.setInt(4, hall.getHallId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteHall(int hallId) throws SQLException {
        String sql = "DELETE FROM halls WHERE hall_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, hallId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Returns hall IDs already assigned to a given event. */
    public List<Integer> getHallIdsForEvent(int eventId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT hall_id FROM event_hall WHERE event_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("hall_id"));
            }
        }
        return ids;
    }

    public List<Hall> getHallsForEvent(int eventId) throws SQLException {
        List<Hall> list = new ArrayList<>();
        String sql =
            "SELECT h.hall_id, h.name, h.price_per_day, h.capacity " +
            "FROM halls h JOIN event_hall eh ON h.hall_id = eh.hall_id " +
            "WHERE eh.event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Hall(
                        rs.getInt("hall_id"),
                        rs.getString("name"),
                        rs.getDouble("price_per_day"),
                        rs.getInt("capacity")
                    ));
                }
            }
        }
        return list;
    }
}
