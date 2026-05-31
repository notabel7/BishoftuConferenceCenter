package com.conferenceCenter.dao;

import com.conferenceCenter.model.Hall;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HallDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Returns all halls including a live booking count (how many events currently
     * use each hall). The count drives the "Status" column in HallPanel and the
     * availability hint in the Event dialog's hall picker.
     */
    public List<Hall> getAllHalls() throws SQLException {
        List<Hall> list = new ArrayList<>();
        String sql =
            "SELECT h.hall_id, h.name, h.price_per_day, h.capacity, " +
            "       COUNT(eh.event_id) AS booking_count " +
            "FROM halls h " +
            "LEFT JOIN event_hall eh ON h.hall_id = eh.hall_id " +
            "GROUP BY h.hall_id " +
            "ORDER BY h.hall_id";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Hall h = new Hall(
                    rs.getInt("hall_id"),
                    rs.getString("name"),
                    rs.getDouble("price_per_day"),
                    rs.getInt("capacity")
                );
                h.setBookingCount(rs.getInt("booking_count"));
                list.add(h);
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

    /**
     * Returns total seats already reserved for a hall across all events whose
     * dates overlap with [startDate, endDate], optionally excluding one event
     * (pass 0 to exclude nothing; pass the current eventId when editing so we
     * don't count the event's own existing booking against itself).
     *
     * Overlap condition (standard interval algebra):
     *   existing.start_date <= newEnd  AND  existing.end_date >= newStart
     */
    public int getSeatsBookedForHall(int hallId, String startDate, String endDate,
                                     int excludeEventId) throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(eh.seats_requested), 0) AS seats_used " +
            "FROM event_hall eh " +
            "JOIN events ev ON ev.event_id = eh.event_id " +
            "WHERE eh.hall_id = ? " +
            "  AND ev.start_date <= ? " +
            "  AND ev.end_date   >= ? " +
            "  AND ev.event_id   != ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1,    hallId);
            ps.setString(2, endDate);    // other.start <= our end
            ps.setString(3, startDate);  // other.end   >= our start
            ps.setInt(4,    excludeEventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Returns a map of hallId → seatsRequested for all halls booked by a given event.
     * Used to pre-fill the seat spinners when editing an event.
     */
    public java.util.Map<Integer, Integer> getHallSeatsForEvent(int eventId) throws SQLException {
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        String sql = "SELECT hall_id, seats_requested FROM event_hall WHERE event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("hall_id"), rs.getInt("seats_requested"));
            }
        }
        return map;
    }

    /**
     * Returns all halls booked for a given event, including the seats_requested
     * value from the event_hall junction table. This is critical for the Details
     * view — it must show how many seats THIS event reserved, not the hall's
     * total capacity.
     */
    public List<Hall> getHallsForEvent(int eventId) throws SQLException {
        List<Hall> list = new ArrayList<>();
        String sql =
            "SELECT h.hall_id, h.name, h.price_per_day, h.capacity, eh.seats_requested " +
            "FROM halls h " +
            "JOIN event_hall eh ON h.hall_id = eh.hall_id " +
            "WHERE eh.event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Hall h = new Hall(
                        rs.getInt("hall_id"),
                        rs.getString("name"),
                        rs.getDouble("price_per_day"),
                        rs.getInt("capacity")
                    );
                    h.setSeatsRequested(rs.getInt("seats_requested"));
                    list.add(h);
                }
            }
        }
        return list;
    }
}
