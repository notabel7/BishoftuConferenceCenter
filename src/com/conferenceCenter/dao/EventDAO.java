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
            "SELECT event_id, name, type, owner_first_name, owner_last_name, owner_phone, " +
            "       start_date, end_date " +
            "FROM events ORDER BY start_date, event_id";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Event ev = new Event(
                    rs.getInt("event_id"),
                    rs.getString("name"),
                    rs.getString("type"),
                    rs.getString("owner_first_name"),
                    rs.getString("owner_last_name"),
                    rs.getString("owner_phone"),
                    rs.getString("start_date"),
                    rs.getString("end_date")
                );
                list.add(ev);
            }
        }
        HallDAO hallDAO = new HallDAO();
        for (Event ev : list) {
            ev.setHalls(hallDAO.getHallsForEvent(ev.getEventId()));
        }
        return list;
    }

    public Event getEventById(int eventId) throws SQLException {
        String sql =
            "SELECT event_id, name, type, owner_first_name, owner_last_name, owner_phone, " +
            "       start_date, end_date " +
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
                        rs.getString("owner_phone"),
                        rs.getString("start_date"),
                        rs.getString("end_date")
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

    /**
     * Inserts a new event and links it to the given halls with seat counts.
     * @param hallSeats  map of hallId → seatsRequested (must not be empty)
     */
    public int addEvent(Event ev, java.util.Map<Integer,Integer> hallSeats) throws SQLException {
        conn().setAutoCommit(false);
        try {
            String sql =
                "INSERT INTO events " +
                "  (name, type, owner_first_name, owner_last_name, owner_phone, start_date, end_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            int newId;
            try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, ev.getName());
                ps.setString(2, ev.getType());
                ps.setString(3, ev.getOwnerFirstName());
                ps.setString(4, ev.getOwnerLastName());
                ps.setString(5, ev.getOwnerPhone());
                ps.setString(6, ev.getStartDate());
                ps.setString(7, ev.getEndDate());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    newId = rs.next() ? rs.getInt(1) : -1;
                }
            }
            if (newId < 1) throw new SQLException("Failed to create event.");
            setHallsForEvent(newId, hallSeats);
            conn().commit();
            return newId;
        } catch (SQLException e) {
            conn().rollback();
            throw e;
        } finally {
            conn().setAutoCommit(true);
        }
    }

    /**
     * Updates an existing event and replaces its hall-seat links.
     * @param hallSeats  map of hallId → seatsRequested
     */
    public boolean updateEvent(Event ev, java.util.Map<Integer,Integer> hallSeats) throws SQLException {
        conn().setAutoCommit(false);
        try {
            String sql =
                "UPDATE events SET name=?, type=?, " +
                "  owner_first_name=?, owner_last_name=?, owner_phone=?, " +
                "  start_date=?, end_date=? " +
                "WHERE event_id=?";
            try (PreparedStatement ps = conn().prepareStatement(sql)) {
                ps.setString(1, ev.getName());
                ps.setString(2, ev.getType());
                ps.setString(3, ev.getOwnerFirstName());
                ps.setString(4, ev.getOwnerLastName());
                ps.setString(5, ev.getOwnerPhone());
                ps.setString(6, ev.getStartDate());
                ps.setString(7, ev.getEndDate());
                ps.setInt(8,    ev.getEventId());
                ps.executeUpdate();
            }
            setHallsForEvent(ev.getEventId(), hallSeats);
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
        // event_hall rows are removed automatically by ON DELETE CASCADE.
        // Assigned employees are released automatically by ON DELETE SET NULL on
        // employees.event_id — their event_id becomes NULL (they are NOT deleted).
        String sql = "DELETE FROM events WHERE event_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── Overlap query ─────────────────────────────────────────────────────

    /**
     * Returns names of events whose date range overlaps [startDate, endDate],
     * excluding the given eventId (pass 0 to exclude nothing; pass the current
     * eventId when editing so the event is not compared against itself).
     *
     * Overlap condition: other.start_date <= newEnd AND other.end_date >= newStart
     */
    public java.util.List<String> getOverlappingEventNames(String startDate, String endDate,
                                                            int excludeEventId) throws SQLException {
        java.util.List<String> names = new ArrayList<>();
        String sql =
            "SELECT name FROM events " +
            "WHERE start_date <= ? AND end_date >= ? AND event_id != ? " +
            "ORDER BY start_date";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, endDate);    // other.start <= our end
            ps.setString(2, startDate);  // other.end   >= our start
            ps.setInt(3, excludeEventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) names.add(rs.getString("name"));
            }
        }
        return names;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Atomically replaces all hall links for an event, storing the seat count per hall. */
    private void setHallsForEvent(int eventId,
                                  java.util.Map<Integer,Integer> hallSeats) throws SQLException {
        try (PreparedStatement del = conn().prepareStatement(
                "DELETE FROM event_hall WHERE event_id=?")) {
            del.setInt(1, eventId);
            del.executeUpdate();
        }
        if (hallSeats == null || hallSeats.isEmpty()) return;
        try (PreparedStatement ins = conn().prepareStatement(
                "INSERT INTO event_hall (event_id, hall_id, seats_requested) VALUES (?, ?, ?)")) {
            for (java.util.Map.Entry<Integer,Integer> e : hallSeats.entrySet()) {
                ins.setInt(1, eventId);
                ins.setInt(2, e.getKey());
                ins.setInt(3, e.getValue());
                ins.addBatch();
            }
            ins.executeBatch();
        }
    }
}
