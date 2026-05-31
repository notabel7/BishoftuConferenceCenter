package com.conferenceCenter.dao;

import com.conferenceCenter.model.AssignedEmployee;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the employees table and the employee_event junction table.
 *
 * The employee_event junction table (added in schema v2) replaces the old
 * single event_id column on employees. This means:
 *   - An employee can be assigned to more than one event if business rules change.
 *   - The max-3-employees-per-event rule is still enforced by countEmployeesForEvent().
 *   - Deleting an employee automatically removes all their assignments (CASCADE).
 *   - Deleting an event automatically removes all assignments for that event (CASCADE).
 */
public class EmployeeDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── Queries ───────────────────────────────────────────────────────────

    /**
     * Returns every employee together with their current event assignment (if any).
     * Employees not assigned to any event are included with a null event name.
     * When an employee is assigned to multiple events, one row appears per assignment.
     */
    public List<AssignedEmployee> getAllEmployees() throws SQLException {
        List<AssignedEmployee> list = new ArrayList<>();
        String sql =
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, " +
            "       ee.event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN employee_event ee ON e.employee_id = ee.employee_id " +
            "LEFT JOIN events ev         ON ee.event_id   = ev.event_id " +
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
                    rs.getInt("event_id")          // 0 when LEFT JOIN finds no match
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
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, ee.event_id " +
            "FROM employees e " +
            "JOIN employee_event ee ON e.employee_id = ee.employee_id " +
            "WHERE ee.event_id = ? " +
            "ORDER BY e.last_name, e.first_name";
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

    /** Enforces the max-3-employees-per-event rule. */
    public int countEmployeesForEvent(int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM employee_event WHERE event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ── Mutations ─────────────────────────────────────────────────────────

    /**
     * Inserts a new employee and, if the employee has an event assignment,
     * creates the matching row in employee_event.
     */
    public boolean addEmployee(AssignedEmployee emp) throws SQLException {
        // 1. Insert the employee record (no event_id column any more).
        String insertEmp =
            "INSERT INTO employees " +
            "  (first_name, last_name, phone, years_of_experience, date_of_birth, gender) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(
                insertEmp, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getPhone());
            ps.setInt(4,    emp.getYearsOfExperience());
            ps.setString(5, emp.getDateOfBirth());
            ps.setString(6, emp.getGender());
            if (ps.executeUpdate() == 0) return false;

            // 2. If an event is specified, link them in the junction table.
            if (emp.getEventId() > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int newId = keys.getInt(1);
                        linkToEvent(newId, emp.getEventId());
                    }
                }
            }
        }
        return true;
    }

    /**
     * Updates an employee's personal details and re-links their event assignment.
     * The old assignment is removed first; the new one is inserted if present.
     */
    public boolean updateEmployee(AssignedEmployee emp) throws SQLException {
        // 1. Update personal details.
        String updateEmp =
            "UPDATE employees " +
            "SET first_name=?, last_name=?, phone=?, years_of_experience=?, " +
            "    date_of_birth=?, gender=? " +
            "WHERE employee_id=?";
        try (PreparedStatement ps = conn().prepareStatement(updateEmp)) {
            ps.setString(1, emp.getFirstName());
            ps.setString(2, emp.getLastName());
            ps.setString(3, emp.getPhone());
            ps.setInt(4,    emp.getYearsOfExperience());
            ps.setString(5, emp.getDateOfBirth());
            ps.setString(6, emp.getGender());
            ps.setInt(7,    emp.getEmployeeId());
            if (ps.executeUpdate() == 0) return false;
        }

        // 2. Replace the event assignment (delete old, insert new if present).
        unlinkAllEvents(emp.getEmployeeId());
        if (emp.getEventId() > 0) {
            linkToEvent(emp.getEmployeeId(), emp.getEventId());
        }
        return true;
    }

    /** Deletes an employee. The employee_event rows are removed automatically by CASCADE. */
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
            "       ee.event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN employee_event ee ON e.employee_id = ee.employee_id " +
            "LEFT JOIN events ev         ON ee.event_id   = ev.event_id " +
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
     * Returns all employees (one row per person) for use in the Event dialog's
     * staff picker. Each employee carries their current event assignment name
     * so the picker can show context ("currently assigned to: Tech Summit").
     * Uses GROUP BY so an employee assigned to multiple events appears only once.
     */
    /**
     * Returns all employees (one row per person) for the Event dialog's staff picker.
     * Uses a subquery to find each employee's most-recent event assignment, then
     * JOINs to events to resolve the name — avoids the correlated-subquery+aggregate
     * pattern that SQLite rejects at runtime.
     */
    public List<AssignedEmployee> getAllEmployeesForPicker() throws SQLException {
        List<AssignedEmployee> list = new ArrayList<>();
        String sql =
            "SELECT e.employee_id, e.first_name, e.last_name, e.phone, " +
            "       e.years_of_experience, e.date_of_birth, e.gender, " +
            "       COALESCE(ee2.event_id, 0) AS event_id, ev.name AS event_name " +
            "FROM employees e " +
            "LEFT JOIN ( " +
            "    SELECT employee_id, MAX(event_id) AS event_id " +
            "    FROM employee_event GROUP BY employee_id " +
            ") ee2 ON e.employee_id = ee2.employee_id " +
            "LEFT JOIN events ev ON ev.event_id = ee2.event_id " +
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
        String sql = "SELECT employee_id FROM employee_event WHERE event_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt("employee_id"));
            }
        }
        return ids;
    }

    /**
     * Atomically replaces all employee assignments for an event.
     * Old links are removed; new ones are inserted — in a single transaction.
     * Employee records themselves are never touched.
     *
     * This is the correct pattern for a staff-assignment system:
     * the Event picks from an existing staff pool; it does not own the staff.
     */
    public void setEventEmployees(int eventId, List<Integer> employeeIds) throws SQLException {
        Connection c = conn();
        c.setAutoCommit(false);
        try {
            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM employee_event WHERE event_id = ?")) {
                del.setInt(1, eventId);
                del.executeUpdate();
            }
            if (employeeIds != null && !employeeIds.isEmpty()) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT OR IGNORE INTO employee_event (employee_id, event_id) VALUES (?, ?)")) {
                    for (int empId : employeeIds) {
                        ins.setInt(1, empId);
                        ins.setInt(2, eventId);
                        ins.addBatch();
                    }
                    ins.executeBatch();
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

    // ── Junction-table helpers ────────────────────────────────────────────

    /** Inserts one row into employee_event. */
    private void linkToEvent(int employeeId, int eventId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO employee_event (employee_id, event_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.setInt(2, eventId);
            ps.executeUpdate();
        }
    }

    /** Removes all event assignments for a given employee. */
    private void unlinkAllEvents(int employeeId) throws SQLException {
        String sql = "DELETE FROM employee_event WHERE employee_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, employeeId);
            ps.executeUpdate();
        }
    }
}
