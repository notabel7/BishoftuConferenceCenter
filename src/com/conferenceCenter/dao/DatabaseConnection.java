package com.conferenceCenter.dao;

import java.sql.*;

/**
 * Singleton wrapper around the SQLite connection.
 * Call initializeSchema() once at application startup.
 *
 * Schema design notes (Database normalization):
 *   - 1NF  : All attribute values are atomic. Names are stored as separate
 *             first_name / last_name columns — never as a combined full_name.
 *   - 2NF  : Every non-key attribute depends on the whole primary key.
 *   - 3NF  : No transitive dependencies; event_hall and employee_event are
 *             proper junction tables — no transitive FK buried in a data table.
 *   - CHECK constraints enforce domain integrity at the database level,
 *             so invalid data cannot enter the system even via raw SQL.
 *   - UNIQUE(phone) on employees and admins prevents duplicate registrations
 *             with the same phone number. SQLite treats each NULL as distinct,
 *             so employees without a phone number are unaffected.
 *   - Dates are stored as TEXT in ISO-8601 format (YYYY-MM-DD), enforced by a
 *             GLOB CHECK constraint. This allows lexicographic sorting and
 *             SQLite's built-in date functions to work correctly.
 *
 * NOTE: schema changes only take effect on a fresh database. Delete
 *       conference_center.db and re-run the app to apply them.
 */
public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:conference_center.db";
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "SQLite JDBC driver not found.\n" +
                "Place sqlite-jdbc-*.jar inside the 'lib' folder and recompile.", e);
        }
        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);

        // Enable foreign-key enforcement for every new connection
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    public static DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }

    // ── Schema bootstrap ──────────────────────────────────────────────────

    public static void initializeSchema() throws SQLException {
        Connection c = getInstance().getConnection();
        try (Statement st = c.createStatement()) {

            // ── halls ──────────────────────────────────────────────────
            // price_per_day and capacity must be positive (CHECK constraints).
            st.execute(
                "CREATE TABLE IF NOT EXISTS halls (" +
                "  hall_id       INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name          TEXT    NOT NULL UNIQUE," +
                "  price_per_day REAL    NOT NULL CHECK(price_per_day > 0)," +
                "  capacity      INTEGER NOT NULL CHECK(capacity > 0)" +
                ")"
            );

            // ── events ────────────────────────────────────────────────
            // Owner name is split into first_name + last_name (1NF atomicity).
            st.execute(
                "CREATE TABLE IF NOT EXISTS events (" +
                "  event_id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  name              TEXT    NOT NULL," +
                "  type              TEXT    NOT NULL," +
                "  owner_first_name  TEXT    NOT NULL," +
                "  owner_last_name   TEXT    NOT NULL," +
                "  owner_phone       TEXT    NOT NULL" +
                ")"
            );

            // ── event_hall (junction / associative table) ─────────────
            // Resolves the many-to-many relationship between events and halls.
            st.execute(
                "CREATE TABLE IF NOT EXISTS event_hall (" +
                "  event_id INTEGER NOT NULL," +
                "  hall_id  INTEGER NOT NULL," +
                "  PRIMARY KEY (event_id, hall_id)," +
                "  FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE," +
                "  FOREIGN KEY (hall_id)  REFERENCES halls(hall_id)   ON DELETE CASCADE" +
                ")"
            );

            // ── employees ─────────────────────────────────────────────
            // Name split into first_name + last_name (1NF).
            // gender restricted to 'Male'/'Female' via CHECK.
            // years_of_experience cannot be negative.
            // phone is UNIQUE — SQLite treats each NULL as distinct, so employees
            //   without a phone number are still allowed in any quantity.
            // date_of_birth uses a GLOB CHECK to enforce ISO-8601 (YYYY-MM-DD).
            // event_id removed — assignments live in the employee_event junction table.
            st.execute(
                "CREATE TABLE IF NOT EXISTS employees (" +
                "  employee_id         INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  first_name          TEXT    NOT NULL," +
                "  last_name           TEXT    NOT NULL," +
                "  phone               TEXT    UNIQUE," +
                "  years_of_experience INTEGER NOT NULL CHECK(years_of_experience >= 0)," +
                "  date_of_birth       TEXT    NOT NULL" +
                "    CHECK(date_of_birth GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')," +
                "  gender              TEXT    CHECK(gender IN ('Male','Female'))" +
                ")"
            );

            // ── employee_event (junction table) ───────────────────────
            // Replaces the single event_id FK on employees.
            // An employee can now be assigned to multiple events; the max-3
            // rule is still enforced in EmployeeDAO.countEmployeesForEvent().
            // Cascade deletes: removing an employee or event cleans up assignments.
            st.execute(
                "CREATE TABLE IF NOT EXISTS employee_event (" +
                "  employee_id INTEGER NOT NULL," +
                "  event_id    INTEGER NOT NULL," +
                "  PRIMARY KEY (employee_id, event_id)," +
                "  FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE," +
                "  FOREIGN KEY (event_id)    REFERENCES events(event_id)       ON DELETE CASCADE" +
                ")"
            );

            // ── admins ────────────────────────────────────────────────
            // Same 1NF name split as employees.
            // username must be unique (natural key for authentication).
            // phone is UNIQUE — same NULL-safe reasoning as employees.
            // date_of_birth enforces ISO-8601 via GLOB CHECK.
            st.execute(
                "CREATE TABLE IF NOT EXISTS admins (" +
                "  admin_id            INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  first_name          TEXT    NOT NULL," +
                "  last_name           TEXT    NOT NULL," +
                "  phone               TEXT    UNIQUE," +
                "  years_of_experience INTEGER NOT NULL DEFAULT 0 CHECK(years_of_experience >= 0)," +
                "  date_of_birth       TEXT    NOT NULL DEFAULT '1900-01-01'" +
                "    CHECK(date_of_birth GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]')," +
                "  gender              TEXT    CHECK(gender IN ('Male','Female'))," +
                "  username            TEXT    NOT NULL UNIQUE," +
                "  password            TEXT    NOT NULL" +
                ")"
            );

            // ── Seed: default admin account ───────────────────────────
            ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM admins");
            if (rs.next() && rs.getInt(1) == 0) {
                st.execute(
                    "INSERT INTO admins " +
                    "  (first_name, last_name, phone, years_of_experience, date_of_birth, gender, username, password) " +
                    "VALUES " +
                    "  ('System','Administrator','0900000000',5,'1990-01-01','Male','admin','admin123')"
                );
            }
            rs.close();

            // ── Seed: five halls ──────────────────────────────────────
            rs = st.executeQuery("SELECT COUNT(*) FROM halls");
            if (rs.next() && rs.getInt(1) == 0) {
                String[] inserts = {
                    "INSERT INTO halls (name,price_per_day,capacity) VALUES ('Abay Hall',   5000.00, 500)",
                    "INSERT INTO halls (name,price_per_day,capacity) VALUES ('Baro Hall',   3000.00, 300)",
                    "INSERT INTO halls (name,price_per_day,capacity) VALUES ('Awash Hall',  2000.00, 200)",
                    "INSERT INTO halls (name,price_per_day,capacity) VALUES ('Omo Hall',    1500.00, 150)",
                    "INSERT INTO halls (name,price_per_day,capacity) VALUES ('Hawassa Hall',1000.00, 100)"
                };
                for (String sql : inserts) st.execute(sql);
            }
            rs.close();
        }
    }
}
