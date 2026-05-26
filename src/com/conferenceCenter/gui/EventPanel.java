package com.conferenceCenter.gui;

import com.conferenceCenter.dao.EventDAO;
import com.conferenceCenter.dao.HallDAO;
import com.conferenceCenter.dao.EmployeeDAO;
import com.conferenceCenter.model.AssignedEmployee;
import com.conferenceCenter.model.Event;
import com.conferenceCenter.model.Hall;
import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EventPanel extends JPanel {

    private final EventDAO    eventDAO    = new EventDAO();
    private final HallDAO     hallDAO     = new HallDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    private final String[] COLS = {
        "ID", "Event Name", "Type", "Owner First Name", "Owner Last Name", "Owner Phone", "Halls Booked"
    };
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable     table    = new JTable(model);
    private final JTextField tfSearch = new JTextField(18);

    public EventPanel() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BACKGROUND);
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Title bar ────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIConstants.PRIMARY);
        titleBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("Event and Conference Management");
        title.setFont(UIConstants.FONT_H2);
        title.setForeground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);
        JLabel sl = new JLabel("Search:");
        sl.setFont(UIConstants.FONT_BOLD);
        sl.setForeground(Color.WHITE);
        tfSearch.setFont(UIConstants.FONT_BODY);
        tfSearch.setPreferredSize(new Dimension(180, 28));
        JButton btnSearch = new JButton();
        applyIcon(btnSearch, "search.png");
        btnSearch.setFocusPainted(false);
        btnSearch.setMargin(new Insets(4, 8, 4, 8));
        btnSearch.addActionListener(e -> filterTable());
        tfSearch.addActionListener(e -> filterTable());
        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });

        searchPanel.add(sl);
        searchPanel.add(tfSearch);
        searchPanel.add(btnSearch);

        titleBar.add(title,       BorderLayout.WEST);
        titleBar.add(searchPanel, BorderLayout.EAST);

        // ── Table ────────────────────────────────────────────────────
        styleTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(UIConstants.BACKGROUND);
        center.setBorder(new EmptyBorder(14, 18, 10, 18));
        center.add(scroll, BorderLayout.CENTER);

        // ── Buttons ──────────────────────────────────────────────────
        JButton btnAdd     = actionButton("Add Event", new Color(199, 134, 30), Color.WHITE);
        JButton btnEdit    = actionButton("Edit",      new Color(21,101,192), Color.WHITE);
        JButton btnDelete  = actionButton("Delete",    UIConstants.DANGER,    Color.WHITE);
        JButton btnDetails = actionButton("Details",   new Color(74,20,140),  Color.WHITE);
        JButton btnRefresh = actionButton("Refresh",   new Color(46,125,50),  Color.WHITE);
        applyIcon(btnAdd,    "add_event.png");
        applyIcon(btnEdit,   "edit.png");
        applyIcon(btnDelete, "delete.png");
        applyIcon(btnDetails,"details.png");
        applyIcon(btnRefresh,"refresh.png");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnPanel.setBackground(UIConstants.BACKGROUND);
        btnPanel.setBorder(new EmptyBorder(0, 18, 12, 18));
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnDetails);
        btnPanel.add(btnRefresh);

        add(titleBar,  BorderLayout.NORTH);
        add(center,    BorderLayout.CENTER);
        add(btnPanel,  BorderLayout.SOUTH);

        btnAdd.addActionListener(e     -> showEventDialog(null));
        btnEdit.addActionListener(e    -> editSelected());
        btnDelete.addActionListener(e  -> deleteSelected());
        btnDetails.addActionListener(e -> viewDetails());
        btnRefresh.addActionListener(e -> loadData());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) viewDetails();
            }
        });
    }

    // ── Data ─────────────────────────────────────────────────────────────

    private void loadData() {
        model.setRowCount(0);
        try {
            for (Event ev : eventDAO.getAllEvents()) {
                model.addRow(new Object[]{
                    ev.getEventId(), ev.getName(), ev.getType(),
                    ev.getOwnerFirstName(), ev.getOwnerLastName(),
                    ev.getOwnerPhone(), ev.getHallNames()
                });
            }
        } catch (Exception ex) {
            error("Failed to load events: " + ex.getMessage());
        }
    }

    private void filterTable() {
        String q = tfSearch.getText().trim().toLowerCase();
        model.setRowCount(0);
        try {
            for (Event ev : eventDAO.getAllEvents()) {
                boolean match = q.isEmpty()
                    || ev.getName().toLowerCase().contains(q)
                    || ev.getType().toLowerCase().contains(q)
                    || ev.getOwnerFirstName().toLowerCase().contains(q)
                    || ev.getOwnerLastName().toLowerCase().contains(q)
                    || ev.getOwnerFullName().toLowerCase().contains(q);
                if (match) {
                    model.addRow(new Object[]{
                        ev.getEventId(), ev.getName(), ev.getType(),
                        ev.getOwnerFirstName(), ev.getOwnerLastName(),
                        ev.getOwnerPhone(), ev.getHallNames()
                    });
                }
            }
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private int selectedEventId() {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        return (int) model.getValueAt(row, 0);
    }

    private void editSelected() {
        int id = selectedEventId();
        if (id < 0) { info("Select an event first."); return; }
        try {
            Event ev = eventDAO.getEventById(id);
            if (ev != null) showEventDialog(ev);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void deleteSelected() {
        int id = selectedEventId();
        if (id < 0) { info("Select an event first."); return; }
        String name = (String) model.getValueAt(table.getSelectedRow(), 1);
        int res = JOptionPane.showConfirmDialog(
            this, "Delete event \"" + name + "\" and all its assignments?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            eventDAO.deleteEvent(id);
            loadData();
            JOptionPane.showMessageDialog(this, "Event deleted.", "Deleted",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void viewDetails() {
        int id = selectedEventId();
        if (id < 0) { info("Select an event first."); return; }
        try {
            Event ev = eventDAO.getEventById(id);
            if (ev == null) return;
            showDetailsDialog(ev);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    // ── Event Add/Edit dialog ─────────────────────────────────────────────

    private void showEventDialog(Event existing) {
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Event" : "Add New Event",
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(600, 660);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(formHeader(isEdit ? "Edit Event" : "New Event / Conference"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIConstants.FONT_BOLD);

        // ── Tab 1: Basic Info ─────────────────────────────────────────
        JTextField tfName           = formField(isEdit ? existing.getName() : "");
        JComboBox<String> cbType    = new JComboBox<>(new String[]{
            "Conference", "Seminar", "Workshop", "Wedding", "Birthday",
            "Corporate Meeting", "Training", "Exhibition", "Other"
        });
        if (isEdit) cbType.setSelectedItem(existing.getType());
        cbType.setFont(UIConstants.FONT_BODY);

        JTextField tfOwnerFirstName = formField(isEdit ? existing.getOwnerFirstName() : "");
        JTextField tfOwnerLastName  = formField(isEdit ? existing.getOwnerLastName()  : "");
        JTextField tfOwnerPhone     = formField(isEdit ? existing.getOwnerPhone() : "");

        JPanel tabBasic = new JPanel(new GridBagLayout());
        tabBasic.setBackground(Color.WHITE);
        tabBasic.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(7,4,7,4);

        Object[][] basicRows = {
            {"Event / Conference Name *", tfName},
            {"Event Type *",              cbType},
            {"Owner First Name *",        tfOwnerFirstName},
            {"Owner Last Name *",         tfOwnerLastName},
            {"Owner Phone *",             tfOwnerPhone}
        };
        for (int i = 0; i < basicRows.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0.38;
            JLabel lbl = fLabel((String) basicRows[i][0]);
            tabBasic.add(lbl, gc);
            gc.gridx = 1; gc.weightx = 0.62;
            tabBasic.add((Component) basicRows[i][1], gc);
        }

        // ── Tab 2: Hall Selection ─────────────────────────────────────
        List<Hall> allHalls;
        List<Integer> existingHallIds = new ArrayList<>();
        try {
            allHalls = hallDAO.getAllHalls();
            if (isEdit) existingHallIds = hallDAO.getHallIdsForEvent(existing.getEventId());
        } catch (Exception ex) {
            allHalls = new ArrayList<>();
        }

        JPanel tabHalls = new JPanel();
        tabHalls.setBackground(Color.WHITE);
        tabHalls.setBorder(new EmptyBorder(12, 20, 12, 20));
        tabHalls.setLayout(new BoxLayout(tabHalls, BoxLayout.Y_AXIS));

        JLabel hallNote = new JLabel("Select one or more halls for this event:");
        hallNote.setFont(UIConstants.FONT_BOLD);
        hallNote.setForeground(UIConstants.PRIMARY);
        hallNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabHalls.add(hallNote);
        tabHalls.add(Box.createVerticalStrut(10));

        List<JCheckBox> hallChecks = new ArrayList<>();
        for (Hall h : allHalls) {
            JCheckBox cb = new JCheckBox(
                h.getName() + "  —  " + h.getCapacity() + " seats  |  ETB " +
                String.format("%.2f", h.getPricePerDay()) + "/day"
            );
            cb.setFont(UIConstants.FONT_BODY);
            cb.setBackground(Color.WHITE);
            cb.setAlignmentX(Component.LEFT_ALIGNMENT);
            cb.putClientProperty("hallId", h.getHallId());
            if (existingHallIds.contains(h.getHallId())) cb.setSelected(true);
            hallChecks.add(cb);
            tabHalls.add(cb);
            tabHalls.add(Box.createVerticalStrut(4));
        }

        // ── Tab 3: Employee Assignment ────────────────────────────────
        List<AssignedEmployee> existingEmpsTemp = new ArrayList<>();
        if (isEdit) {
            try { existingEmpsTemp = employeeDAO.getEmployeesByEvent(existing.getEventId()); }
            catch (Exception ex) { /* leave empty */ }
        }
        final List<AssignedEmployee> existingEmps = existingEmpsTemp;

        JPanel tabEmployees = new JPanel(new BorderLayout());
        tabEmployees.setBackground(Color.WHITE);
        tabEmployees.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel empNote = new JLabel("Assign up to 3 employees to this event:");
        empNote.setFont(UIConstants.FONT_BOLD);
        empNote.setForeground(UIConstants.PRIMARY);
        tabEmployees.add(empNote, BorderLayout.NORTH);

        // Three employee sub-panels (each with first/last/phone/gender/exp/dob)
        JPanel empForms = new JPanel(new GridLayout(3, 1, 0, 10));
        empForms.setBackground(Color.WHITE);
        empForms.setBorder(new EmptyBorder(10, 0, 0, 0));

        // empRows: [eFirstName, eLastName, ePhone, eYoe, eDob], empGenders: JComboBox per row
        List<JTextField[]>     empRows    = new ArrayList<>();
        List<JComboBox<String>> empGenders = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            JPanel row = new JPanel(new GridBagLayout());
            row.setBackground(Color.WHITE);
            TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                "Employee " + (i + 1)
            );
            tb.setTitleFont(UIConstants.FONT_BOLD);
            tb.setTitleColor(UIConstants.PRIMARY);
            row.setBorder(tb);

            GridBagConstraints eg = new GridBagConstraints();
            eg.fill = GridBagConstraints.HORIZONTAL; eg.insets = new Insets(3,4,3,4);

            JTextField eFirstName = formField("");
            JTextField eLastName  = formField("");
            JTextField ePhone     = formField("");
            JTextField eYoe       = formField("");
            JTextField eDob       = formField("");
            eDob.setToolTipText("Format: YYYY-MM-DD  e.g. 1995-08-23");

            JComboBox<String> eGender = new JComboBox<>(new String[]{"", "Male", "Female"});
            eGender.setFont(UIConstants.FONT_BODY);

            if (i < existingEmps.size()) {
                AssignedEmployee ae = existingEmps.get(i);
                eFirstName.setText(ae.getFirstName());
                eLastName.setText(ae.getLastName());
                ePhone.setText(ae.getPhone() == null ? "" : ae.getPhone());
                eYoe.setText(String.valueOf(ae.getYearsOfExperience()));
                eDob.setText(ae.getDateOfBirth() == null ? "" : ae.getDateOfBirth());
                if (ae.getGender() != null) eGender.setSelectedItem(ae.getGender());
            }

            // Row 0: First Name | Last Name
            eg.gridx=0; eg.gridy=0; eg.weightx=0.22; row.add(fLabel("First Name:"), eg);
            eg.gridx=1; eg.weightx=0.30;              row.add(eFirstName, eg);
            eg.gridx=2; eg.weightx=0.22;              row.add(fLabel("Last Name:"), eg);
            eg.gridx=3; eg.weightx=0.30;              row.add(eLastName, eg);
            // Row 1: Phone | Gender
            eg.gridx=0; eg.gridy=1; eg.weightx=0.22; row.add(fLabel("Phone:"), eg);
            eg.gridx=1; eg.weightx=0.30;              row.add(ePhone, eg);
            eg.gridx=2; eg.weightx=0.22;              row.add(fLabel("Gender:"), eg);
            eg.gridx=3; eg.weightx=0.30;              row.add(eGender, eg);
            // Row 2: Experience | DOB
            eg.gridx=0; eg.gridy=2; eg.weightx=0.22; row.add(fLabel("Exp (yrs):"), eg);
            eg.gridx=1; eg.weightx=0.30;              row.add(eYoe, eg);
            eg.gridx=2; eg.weightx=0.22;              row.add(fLabel("DOB:"), eg);
            eg.gridx=3; eg.weightx=0.30;              row.add(eDob, eg);

            empRows.add(new JTextField[]{ eFirstName, eLastName, ePhone, eYoe, eDob });
            empGenders.add(eGender);
            empForms.add(row);
        }
        tabEmployees.add(new JScrollPane(empForms), BorderLayout.CENTER);

        tabs.addTab("Basic Info",   tabBasic);
        tabs.addTab("Hall Booking", new JScrollPane(tabHalls));
        tabs.addTab("Employees",    tabEmployees);

        // ── Buttons ──────────────────────────────────────────────────
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(Color.WHITE);
        JButton btnCancel = actionButton("Cancel",      new Color(200,200,200), Color.DARK_GRAY);
        JButton btnSave   = actionButton("  Save  ",    UIConstants.SECONDARY,  UIConstants.PRIMARY);
        btns.add(btnCancel);
        btns.add(btnSave);

        root.add(tabs, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        dlg.setContentPane(root);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            // Validate basic info
            String name           = tfName.getText().trim();
            String type           = (String) cbType.getSelectedItem();
            String ownerFirstName = tfOwnerFirstName.getText().trim();
            String ownerLastName  = tfOwnerLastName.getText().trim();
            String ownerPhone     = tfOwnerPhone.getText().trim();

            if (name.isEmpty() || ownerFirstName.isEmpty() || ownerLastName.isEmpty() || ownerPhone.isEmpty()) {
                error("Event name, owner first name, last name and phone are required."); return;
            }
            if (hasDigit(name)) {
                error("Event name cannot contain numbers."); return;
            }
            if (hasDigit(ownerFirstName) || hasDigit(ownerLastName)) {
                error("Owner name fields cannot contain numbers."); return;
            }
            if (!isValidPhone(ownerPhone)) {
                error("Owner phone must contain only digits."); return;
            }

            // Collect selected halls
            List<Integer> selectedHallIds = new ArrayList<>();
            for (JCheckBox cb : hallChecks) {
                if (cb.isSelected()) {
                    selectedHallIds.add((int) cb.getClientProperty("hallId"));
                }
            }
            if (selectedHallIds.isEmpty()) {
                error("Select at least one hall."); return;
            }

            try {
                // Save / update event
                Event ev = isEdit ? existing : new Event();
                ev.setName(name);
                ev.setType(type);
                ev.setOwnerFirstName(ownerFirstName);
                ev.setOwnerLastName(ownerLastName);
                ev.setOwnerPhone(ownerPhone);

                int eventId;
                if (isEdit) {
                    eventDAO.updateEvent(ev, selectedHallIds);
                    eventId = existing.getEventId();
                    // Remove old employees so they can be re-added below
                    for (AssignedEmployee ae : existingEmps) {
                        employeeDAO.deleteEmployee(ae.getEmployeeId());
                    }
                } else {
                    eventId = eventDAO.addEvent(ev, selectedHallIds);
                }

                // Save employees (only rows where at least a first name is filled)
                for (int i = 0; i < empRows.size(); i++) {
                    JTextField[] empRow = empRows.get(i);
                    String eFirstName = empRow[0].getText().trim();
                    String eLastName  = empRow[1].getText().trim();
                    if (eFirstName.isEmpty() && eLastName.isEmpty()) continue;
                    if (eFirstName.isEmpty() || eLastName.isEmpty()) {
                        error("Both first name and last name are required for each employee."); return;
                    }
                    if (hasDigit(eFirstName) || hasDigit(eLastName)) {
                        error("Employee name fields cannot contain numbers."); return;
                    }
                    String ePhone = empRow[2].getText().trim();
                    if (!ePhone.isEmpty() && !isValidPhone(ePhone)) {
                        error("Employee phone must contain only digits."); return;
                    }
                    String eYoeS = empRow[3].getText().trim();
                    String eDobS = empRow[4].getText().trim();
                    if (eYoeS.isEmpty() || eDobS.isEmpty()) {
                        error("Fill experience and date of birth for \"" + eFirstName + " " + eLastName + "\"."); return;
                    }
                    int yoe;
                    try { yoe = Integer.parseInt(eYoeS); }
                    catch (NumberFormatException ex) {
                        error("Years of experience must be a whole number."); return;
                    }
                    try { java.time.LocalDate.parse(eDobS); }
                    catch (java.time.format.DateTimeParseException ex) {
                        error("Date of birth for \"" + eFirstName + " " + eLastName + "\" must be YYYY-MM-DD."); return;
                    }
                    String eGender = (String) empGenders.get(i).getSelectedItem();
                    AssignedEmployee ae = new AssignedEmployee(
                        0, eFirstName, eLastName,
                        ePhone.isEmpty() ? null : ePhone,
                        yoe, eDobS,
                        (eGender == null || eGender.isEmpty()) ? null : eGender,
                        eventId
                    );
                    employeeDAO.addEmployee(ae);
                }

                dlg.dispose();
                loadData();
                JOptionPane.showMessageDialog(this,
                    "Event " + (isEdit ? "updated" : "created") + " successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                error("Save failed: " + ex.getMessage());
            }
        });

        dlg.setVisible(true);
    }

    // ── Details dialog ────────────────────────────────────────────────────

    private void showDetailsDialog(Event ev) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            "Event Details — " + ev.getName(),
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(540, 500);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(formHeader("Event Details"), BorderLayout.NORTH);

        JTextArea ta = new JTextArea();
        ta.setFont(UIConstants.FONT_BODY);
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setMargin(new Insets(14, 16, 14, 16));

        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("  EVENT INFORMATION\n");
        sb.append("===========================================\n\n");
        sb.append(String.format("  %-20s %s%n",  "Event Name:",       ev.getName()));
        sb.append(String.format("  %-20s %s%n",  "Type:",             ev.getType()));
        sb.append(String.format("  %-20s %s%n",  "Owner First Name:", ev.getOwnerFirstName()));
        sb.append(String.format("  %-20s %s%n",  "Owner Last Name:",  ev.getOwnerLastName()));
        sb.append(String.format("  %-20s %s%n",  "Owner Phone:",      ev.getOwnerPhone()));
        sb.append("\n  --- Halls Booked ---\n\n");
        for (Hall h : ev.getHalls()) {
            sb.append(String.format("  * %-20s  %d seats  |  ETB %.2f/day%n",
                h.getName(), h.getCapacity(), h.getPricePerDay()));
        }
        sb.append("\n  --- Assigned Employees ---\n\n");
        if (ev.getEmployees().isEmpty()) {
            sb.append("  No employees assigned.\n");
        } else {
            for (AssignedEmployee ae : ev.getEmployees()) {
                sb.append(String.format(
                    "  * %-24s  Phone: %-14s  Exp: %d yrs  DOB: %s%n",
                    ae.getFullName(), ae.getPhone() == null ? "—" : ae.getPhone(),
                    ae.getYearsOfExperience(), ae.getDateOfBirth()
                ));
            }
        }
        sb.append("\n===========================================\n");

        ta.setText(sb.toString());
        root.add(new JScrollPane(ta), BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.setBackground(Color.WHITE);
        JButton close = actionButton("  Close  ", UIConstants.PRIMARY, Color.WHITE);
        close.addActionListener(e -> dlg.dispose());
        btns.add(close);
        root.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Styling helpers ───────────────────────────────────────────────────

    private void styleTable() {
        table.setFont(UIConstants.FONT_TABLE);
        table.setRowHeight(UIConstants.ROW_HEIGHT);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UIConstants.SELECTED_ROW);
        table.setSelectionForeground(Color.BLACK);
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);

        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setText(val == null ? "" : val.toString());
                setBackground(UIConstants.PRIMARY);
                setForeground(Color.WHITE);
                setFont(UIConstants.FONT_HEADER);
                setBorder(new EmptyBorder(6, 8, 6, 8));
                setOpaque(true);
                return this;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : UIConstants.TABLE_ALT_ROW);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // ID | Event Name | Type | Owner First | Owner Last | Phone | Halls
        int[] widths = {40, 180, 110, 130, 130, 110, 180};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
    }

    private JPanel formHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        p.setBackground(UIConstants.PRIMARY);
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_H2);
        l.setForeground(Color.WHITE);
        p.add(l);
        return p;
    }

    private JTextField formField(String val) {
        JTextField f = new JTextField(val);
        f.setFont(UIConstants.FONT_BODY);
        f.setBackground(UIConstants.INPUT_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        f.setPreferredSize(new Dimension(0, UIConstants.FIELD_H));
        return f;
    }

    private JLabel fLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BOLD);
        l.setForeground(UIConstants.PRIMARY);
        return l;
    }

    private JButton actionButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(UIConstants.FONT_BUTTON);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setIconTextGap(6);
        return b;
    }

    private void applyIcon(JButton btn, String iconFile) {
        javax.swing.ImageIcon ic = UIConstants.loadIcon(iconFile);
        if (ic != null) btn.setIcon(ic);
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d+");
    }

    private boolean hasDigit(String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}
