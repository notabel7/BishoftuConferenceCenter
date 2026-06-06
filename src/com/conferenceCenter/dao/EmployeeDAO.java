package com.conferenceCenter.dao;

import com.conferenceCenter.model.AssignedEmployee;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the employees table.
 *
 * Event assignments are stored directly on employees.event_id (a nullable
 * foreign key to events). This models the relationship as one-to-many:
 *   - One event has many employees (up to 3, enforced in the application layer).
 *   - Each employee belongs to at most one event — a single column physically
 *     cannot hold two events, so "one event per employee" is guaranteed by the
 *     schema, not just by code.
 *   - Deleting an event releases its staff via ON DELETE SET NULL (their
 *     event_id becomes NULL); the employees themselves are never deleted.
 */
public class EmployeeDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Returns every employee together with their current event assignment (if any).
     * Employees not assigned to any event are included with a null event name.
     * Exactly one row per employee — guaranteed by the single event_id column.
     */
    public List<AssignedEmployee> getAllEmployees() throws SQLException {
        List<AssignedEmployee> list = new ArrayList<>();
        String sql =
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, " +
            "       e.event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN events ev ON e.event_id = ev.event_id " +
            "ORDER BY e.last_name, e.first_name";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AssignedEmployee emp = new AssignedEmployee(
                    rs.getInt("employee_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("phone"),
                    rs.getInt("years_of_experience"),
                    rs.getString("date_of_birth"),
                    rs.getString("gender"),
                    rs.getInt("event_id")          // 0 when event_id is NULL
                );
                emp.setEventName(rs.getString("event_name"));
                list.add(emp);
            }
        }
        return list;
    }

    /** Returns all employees assigned to the given event. */
    public List<AssignedEmployee> getEmployeesByEvent(int eventId) throws SQLException {
        List<AssignedEmployee> list = new ArrayList<>();
        String sql =
            "SELECT employee_id, first_name, last_name, phone, " +
            "       years_of_experience, date_of_birth, gender, event_id " +
            "FROM employees WHERE event_id = ? " +
            "ORDER BY last_name, first_name";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AssignedEmployee(
                        rs.getInt("employee_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getInt("years_of_experience"),
                        rs.getString("date_of_birth"),
                        rs.getString("gender"),
                        rs.getInt("event_id")
                    ));
                }
            }
        }
        return list;
    }

    /** Enforces the max-3-employees-per-event rule (counted in the application layer). */
    public int countEmployeesForEvent(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM employees WHERE event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /** Inserts a new employee, writing their event assignment directly into event_id. */
    public boolean addEmployee(AssignedEmployee emp) throws SQLException {
        String sql =
            "INSERT INTO employees " +
            "  (first_name, last_name, phone, years_of_experience, date_of_birth, gender, event_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getPhone());
            ps.setInt(4,    emp.getYearsOfExperience());
            ps.setString(5, emp.getDateOfBirth());
            ps.setString(6, emp.getGender());
            if (emp.getEventId() > 0) ps.setInt(7, emp.getEventId());
            else                      ps.setNull(7, java.sql.Types.INTEGER);
            return ps.executeUpdate() > 0;
        }
    }

    /** Updates an employee's details and event assignment in a single statement. */
    public boolean updateEmployee(AssignedEmployee emp) throws SQLException {
        String sql =
            "UPDATE employees " +
            "SET first_name=?, last_name=?, phone=?, years_of_experience=?, " +
            "    date_of_birth=?, gender=?, event_id=? " +
            "WHERE employee_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getPhone());
            ps.setInt(4,    emp.getYearsOfExperience());
            ps.setString(5, emp.getDateOfBirth());
            ps.setString(6, emp.getGender());
            if (emp.getEventId() > 0) ps.setInt(7, emp.getEventId());
            else                      ps.setNull(7, java.sql.Types.INTEGER);
            ps.setInt(8, emp.getEmployeeId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Deletes an employee by ID. No junction rows to clean up. */
    public boolean deleteEmployee(int employeeId) throws SQLException {
        String sql = "DELETE FROM employees WHERE employee_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Returns a single employee by ID, including their current event assignment.
     * Used by EmployeePanel's edit dialog to pre-populate the event dropdown correctly.
     */
    public AssignedEmployee getEmployeeById(int employeeId) throws SQLException {
        String sql =
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, " +
            "       e.event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN events ev ON e.event_id = ev.event_id " +
            "WHERE e.employee_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AssignedEmployee ae = new AssignedEmployee(
                        rs.getInt("employee_id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("phone"),
                        rs.getInt("years_of_experience"),
                        rs.getString("date_of_birth"),
                        rs.getString("gender"),
                        rs.getInt("event_id")
                    );
                    ae.setEventName(rs.getString("event_name"));
                    return ae;
                }
            }
        }
        return null;
    }

    /**
     * Returns all employees (one row per person) for the Event dialog's staff picker,
     * each carrying their current event assignment name so the picker can show context.
     * No subquery needed — the single event_id column guarantees one row per employee.
     */
    public List<AssignedEmployee> getAllEmployeesForPicker() throws SQLException {
        List<AssignedEmployee> list = new ArrayList<>();
        String sql =
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, " +
            "       COALESCE(e.event_id, 0) AS event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN events ev ON e.event_id = ev.event_id " +
            "ORDER BY e.last_name, e.first_name";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                AssignedEmployee ae = new AssignedEmployee(
                    rs.getInt("employee_id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("phone"),
                    rs.getInt("years_of_experience"),
                    rs.getString("date_of_birth"),
                    rs.getString("gender"),
                    rs.getInt("event_id")
                );
                ae.setEventName(rs.getString("event_name"));
                list.add(ae);
            }
        }
        return list;
    }

    /**
     * Returns the IDs of all employees currently assigned to a given event.
     * Used by the Event dialog to pre-tick the correct checkboxes when editing.
     */
    public List<Integer> getEmployeeIdsForEvent(int eventId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT employee_id FROM employees WHERE event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("employee_id"));
            }
        }
        return ids;
    }

    /**
     * Atomically replaces an event's staff roster.
     *
     * Step 1 releases everyone currently on this event (event_id → NULL).
     * Step 2 assigns the selected employees to this event. Because assignment is
     * a plain UPDATE of event_id, ticking an employee who was on another event
     * MOVES them here and removes them from the old event automatically — which
     * matches the "move them here and remove from that event" confirmation the
     * Staff picker already shows. The whole operation runs in one transaction.
     */
    public void setEventEmployees(int eventId, List<Integer> employeeIds) throws SQLException {
        Connection c = conn();
        c.setAutoCommit(false);
        try {
            // 1. Release every employee currently assigned to this event.
            try (PreparedStatement rel = c.prepareStatement(
                    "UPDATE employees SET event_id = NULL WHERE event_id = ?")) {
                rel.setInt(1, eventId);
                rel.executeUpdate();
            }
            // 2. Assign the selected employees to this event (moves them if needed).
            if (employeeIds != null && !employeeIds.isEmpty()) {
                try (PreparedStatement asg = c.prepareStatement(
                        "UPDATE employees SET event_id = ? WHERE employee_id = ?")) {
                    for (int empId : employeeIds) {
                        asg.setInt(1, eventId);
                        asg.setInt(2, empId);
                        asg.addBatch();
                    }
                    asg.executeBatch();
                }
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
}
