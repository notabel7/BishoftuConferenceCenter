# Bishoftu Conference Center — Management System

Java Swing + SQLite desktop application for managing halls, events, and employees
at a conference center in Bishoftu town.

---

## A. OOP Class Design

### Class Hierarchy (Inheritance Architecture)

```
Person  (abstract)                         ← root: firstName, lastName, phone
 │
 ├── Employee  (abstract)                  ← adds employeeId, yearsOfExperience,
 │    │                                      dateOfBirth, gender
 │    ├── Admin                            ← adds username + password (system manager)
 │    └── AssignedEmployee                 ← adds eventId (linked to a conference)
 │
Hall                                       ← hallId, name, pricePerDay, capacity
Event                                      ← eventId, name, type, ownerFirstName,
                                             ownerLastName, ownerPhone,
                                             List<Hall>, List<AssignedEmployee>
```

**Relationships:**
- `Admin` IS-A `Employee` IS-A `Person`  (single inheritance chain)
- `AssignedEmployee` IS-A `Employee` IS-A `Person`
- `Event` HAS-A `List<Hall>` and `List<AssignedEmployee>`  (composition / association)
- An `Event` can reference multiple `Hall` objects  (one-to-many from event's view)
- Up to 3 `AssignedEmployee` objects belong to one `Event` (enforced in application)

### Packages

| Package                             | Contents                                      |
|-------------------------------------|-----------------------------------------------|
| `com.conferenceCenter.model`        | Entity classes (Person, Employee, Admin …)    |
| `com.conferenceCenter.dao`          | Database access objects (CRUD operations)     |
| `com.conferenceCenter.gui`          | Swing frames and panels                       |
| `com.conferenceCenter.util`         | UIConstants (colours, fonts, sizes)           |

---

## B. Database Schema

```sql
-- Conference halls (5 seeded at startup)
CREATE TABLE halls (
    hall_id       INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL UNIQUE,
    price_per_day REAL    NOT NULL CHECK(price_per_day > 0),
    capacity      INTEGER NOT NULL CHECK(capacity > 0)
);

-- Events / conferences
CREATE TABLE events (
    event_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT NOT NULL,
    type             TEXT NOT NULL,
    owner_first_name TEXT NOT NULL,
    owner_last_name  TEXT NOT NULL,
    owner_phone      TEXT NOT NULL
);

-- Many-to-many: one event can use multiple halls
CREATE TABLE event_hall (
    event_id INTEGER NOT NULL,
    hall_id  INTEGER NOT NULL,
    PRIMARY KEY (event_id, hall_id),
    FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (hall_id)  REFERENCES halls(hall_id)   ON DELETE CASCADE
);

-- Employees (personal details only; assignments live in employee_event)
CREATE TABLE employees (
    employee_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name          TEXT    NOT NULL,
    last_name           TEXT    NOT NULL,
    phone               TEXT    UNIQUE,
    years_of_experience INTEGER NOT NULL CHECK(years_of_experience >= 0),
    date_of_birth       TEXT    NOT NULL   -- ISO-8601: YYYY-MM-DD
        CHECK(date_of_birth GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    gender              TEXT    CHECK(gender IN ('Male','Female'))
);

-- Many-to-many: employees ↔ events (max 3 employees per event, enforced by app)
CREATE TABLE employee_event (
    employee_id INTEGER NOT NULL,
    event_id    INTEGER NOT NULL,
    PRIMARY KEY (employee_id, event_id),
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE,
    FOREIGN KEY (event_id)    REFERENCES events(event_id)       ON DELETE CASCADE
);

-- Admin accounts (manages the system)
CREATE TABLE admins (
    admin_id            INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name          TEXT NOT NULL,
    last_name           TEXT NOT NULL,
    phone               TEXT UNIQUE,
    years_of_experience INTEGER NOT NULL DEFAULT 0 CHECK(years_of_experience >= 0),
    date_of_birth       TEXT NOT NULL DEFAULT '1900-01-01'
        CHECK(date_of_birth GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    gender              TEXT CHECK(gender IN ('Male','Female')),
    username            TEXT NOT NULL UNIQUE,
    password            TEXT NOT NULL
);
```

**Schema design notes:**
- **1NF** — names stored as `first_name` / `last_name`, never as a combined string
- **2NF** — every non-key attribute depends on the whole primary key
- **3NF** — no transitive dependencies; `event_hall` and `employee_event` are proper junction tables
- **CHECK constraints** enforce domain integrity at the database level
- **UNIQUE(phone)** on employees and admins prevents duplicate registrations; NULLs are treated as distinct by SQLite
- **ISO-8601 dates** — enforced by GLOB CHECK; enables correct sorting and SQLite date functions

---

## C. GUI Features

| Screen              | Features                                                             |
|---------------------|----------------------------------------------------------------------|
| **Login**           | Gradient background, Enter-to-login                                  |
| **Main Frame**      | Menu bar (File / Manage / Account / Help) + left-side tabs           |
| **Hall Panel**      | Table with alternating rows, Add / Edit / Delete / Search            |
| **Event Panel**     | 3-tab add dialog (Basic Info, Hall Booking, Employees), Details view |
| **Employee Panel**  | Full CRUD, event assignment dropdown, max-3 rule enforced            |
| **My Profile**      | View logged-in admin's details                                       |
| **Change Password** | Dialog with current / new / confirm fields                           |

**Look & Feel:**
- Primary colour: deep blue `#1a237e`
- Accent colour: gold `#f9a825`
- System Look-and-Feel applied at startup
- Alternating table row colours, header in deep blue with white text

---

## D. How to Run

### Requirements
- Java JDK 11 or later — check with `java --version`
- SQLite JDBC driver JAR
- SVG Salamander JAR (for icon rendering)

### Step 1 — Add the required libraries

Download and place both JARs inside the `lib\` folder, renamed exactly as shown:

| Library | Download | Rename to |
|---------|----------|-----------|
| SQLite JDBC | https://github.com/xerial/sqlite-jdbc/releases/latest | `sqlite-jdbc.jar` |
| SVG Salamander | https://github.com/blackears/svgSalamander/releases | `svgSalamander.jar` |

### Step 2 — Compile
```
.\compile.bat
```

### Step 3 — Run
```
.\run.bat
```

The database file `conference_center.db` is created automatically on first launch.
Five halls are pre-seeded.

---

## E. Default Login

| Username | Password  |
|----------|-----------|
| `admin`  | `admin123` |

> **Important:** This is a local desktop application — the database runs entirely on
> your own machine. Change the admin password after your first login via
> **Account → Change Password** in the menu bar.

---

## F. Project Structure

```
ConferenceCenter/
├── src/
│   ├── Main.java
│   └── com/conferenceCenter/
│       ├── model/       (Person, Employee, Admin, AssignedEmployee, Hall, Event)
│       ├── dao/         (DatabaseConnection, AdminDAO, EmployeeDAO, HallDAO, EventDAO)
│       ├── gui/         (LoginFrame, MainFrame, HallPanel, EventPanel, EmployeePanel)
│       └── util/        (UIConstants)
├── lib/
│   ├── sqlite-jdbc.jar       ← add manually (not in repo)
│   └── svgSalamander.jar     ← add manually (not in repo)
├── resources/
│   └── icons/                (PNG and SVG icon files)
├── compile.bat
├── run.bat
└── README.md
```
