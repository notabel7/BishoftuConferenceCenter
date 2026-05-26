package com.conferenceCenter.dao;

import com.conferenceCenter.model.AssignedEmployee;
import com.conferenceCenter.model.Event;
import com.conferenceCenter.model.Hall;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public List<Event> getAllEvents() throws SQLException {
        List<Event> list = new ArrayList<>();
        String sql =
            "SELECT event_id, name, type, owner_first_name, owner_last_name, owner_phone " +
            "FROM events ORDER BY event_id";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Event ev = new Event(
                    rs.getInt("event_id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("owner_first_name"),
                    rs.getString("owner_last_name"),
                    rs.getString("owner_phone")
                );
                list.add(ev);
            }
        }
        // Attach halls to each event
        HallDAO hallDAO = new HallDAO();
        for (Event ev : list) {
            ev.setHalls(hallDAO.getHallsForEvent(ev.getEventId()));
        }
        return list;
    }

    public Event getEventById(int eventId) throws SQLException {
        String sql =
            "SELECT event_id, name, type, owner_first_name, owner_last_name, owner_phone " +
            "FROM events WHERE event_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Event ev = new Event(
                        rs.getInt("event_id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("owner_first_name"),
                        rs.getString("owner_last_name"),
                        rs.getString("owner_phone")
                    );
                    ev.setHalls(new HallDAO().getHallsForEvent(eventId));
                    ev.setEmployees(new EmployeeDAO().getEmployeesByEvent(eventId));
                    return ev;
                }
            }
        }
        return null;
    }

    // ── Write ─────────────────────────────────────────────────────────────

    public int addEvent(Event ev, List<Integer> hallIds) throws SQLException {
        conn().setAutoCommit(false);
        try {
            String sql =
                "INSERT INTO events (name, type, owner_first_name, owner_last_name, owner_phone) " +
                "VALUES (?, ?, ?, ?, ?)";
            int newId;
            try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, ev.getName());
                ps.setString(2, ev.getType());
                ps.setString(3, ev.getOwnerFirstName());
                ps.setString(4, ev.getOwnerLastName());
                ps.setString(5, ev.getOwnerPhone());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    newId = rs.next() ? rs.getInt(1) : -1;
                }
            }
            if (newId < 1) throw new SQLException("Failed to create event.");

            setHallsForEvent(newId, hallIds);

            conn().commit();
            return newId;
        } catch (SQLException e) {
            conn().rollback();
            throw e;
        } finally {
            conn().setAutoCommit(true);
        }
    }

    public boolean updateEvent(Event ev, List<Integer> hallIds) throws SQLException {
        conn().setAutoCommit(false);
        try {
            String sql =
                "UPDATE events SET name=?, type=?, " +
                "                  owner_first_name=?, owner_last_name=?, owner_phone=? " +
                "WHERE event_id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, ev.getName());
                ps.setString(2, ev.getType());
                ps.setString(3, ev.getOwnerFirstName());
                ps.setString(4, ev.getOwnerLastName());
                ps.setString(5, ev.getOwnerPhone());
                ps.setInt(6, ev.getEventId());
                ps.executeUpdate();
            }
            setHallsForEvent(ev.getEventId(), hallIds);
            conn().commit();
            return true;
        } catch (SQLException e) {
            conn().rollback();
            throw e;
        } finally {
            conn().setAutoCommit(true);
        }
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        // event_hall rows removed automatically by ON DELETE CASCADE
        String sql = "DELETE FROM events WHERE event_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void setHallsForEvent(int eventId, List<Integer> hallIds) throws SQLException {
        try (PreparedStatement del = conn().prepareStatement(
                "DELETE FROM event_hall WHERE event_id=?")) {
            del.setInt(1, eventId);
            del.executeUpdate();
        }
        if (hallIds == null || hallIds.isEmpty()) return;
        try (PreparedStatement ins = conn().prepareStatement(
                "INSERT INTO event_hall (event_id, hall_id) VALUES (?, ?)")) {
            for (int hid : hallIds) {
                ins.setInt(1, eventId);
                ins.setInt(2, hid);
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }
}
