package com.conferenceCenter.gui;

import com.conferenceCenter.dao.EmployeeDAO;
import com.conferenceCenter.dao.EventDAO;
import com.conferenceCenter.model.AssignedEmployee;
import com.conferenceCenter.model.Event;
import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Manages the staff roster — add, edit, and delete employees independently
 * of any event. Employees are resources from a central pool; events pick
 * from this pool rather than creating their own staff.
 *
 * Cross-tab integration: notifies sibling panels after any mutation so the
 * Event dialog's staff picker always reflects the current roster.
 */
public class EmployeePanel extends JPanel {

    private final EmployeeDAO employeeDAO = new EmployeeDAO();
    private final EventDAO    eventDAO    = new EventDAO();

    private final String[] COLS = {
        "ID", "First Name", "Last Name", "Phone", "Experience (yrs)",
        "Date of Birth", "Gender", "Assigned Event"
    };
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable     table    = new JTable(model);
    private final JTextField tfSearch = new JTextField(18);

    /** Notified after any data mutation so sibling panels can refresh. */
    private DataChangeListener onDataChanged;

    public void setOnDataChanged(DataChangeListener listener) {
        this.onDataChanged = listener;
    }

    /** Called by MainFrame's cross-tab wiring to refresh this panel's table. */
    public void refresh() { loadData(); }

    public EmployeePanel() {
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

        JLabel title = new JLabel("Employee Management");
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
        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(searchPanel, BorderLayout.EAST);

        // ── Info banner ───────────────────────────────────────────────
        JLabel infoLbl = new JLabel(
            " Staff roster — assign employees to events from the Events tab.", JLabel.LEFT);
        javax.swing.ImageIcon hintIcon = UIConstants.loadIcon("hint.png");
        if (hintIcon != null) infoLbl.setIcon(hintIcon);
        infoLbl.setIconTextGap(8);
        infoLbl.setFont(UIConstants.FONT_SMALL);
        infoLbl.setForeground(UIConstants.PRIMARY);
        infoLbl.setBackground(new Color(232, 234, 246));
        infoLbl.setOpaque(true);
        infoLbl.setBorder(new EmptyBorder(5, 18, 5, 18));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(titleBar, BorderLayout.NORTH);
        northPanel.add(infoLbl,  BorderLayout.SOUTH);

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
        JButton btnAdd     = actionButton("Add Employee", new Color(199, 134, 30), Color.WHITE);
        JButton btnEdit    = actionButton("Edit",         new Color(21,101,192), Color.WHITE);
        JButton btnDelete  = actionButton("Delete",       UIConstants.DANGER,    Color.WHITE);
        JButton btnRefresh = actionButton("Refresh",      new Color(46,125,50),  Color.WHITE);
        applyIcon(btnAdd,    "add_employees.png");
        applyIcon(btnEdit,   "edit.png");
        applyIcon(btnDelete, "delete.png");
        applyIcon(btnRefresh,"refresh.png");

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        btnPanel.setBackground(UIConstants.BACKGROUND);
        btnPanel.setBorder(new EmptyBorder(0, 18, 12, 18));
        btnPanel.add(btnAdd);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDelete);
        btnPanel.add(btnRefresh);

        add(northPanel, BorderLayout.NORTH);
        add(center,     BorderLayout.CENTER);
        add(btnPanel,   BorderLayout.SOUTH);

        btnAdd.addActionListener(e     -> showDialog(null));
        btnEdit.addActionListener(e    -> editSelected());
        btnDelete.addActionListener(e  -> deleteSelected());
        btnRefresh.addActionListener(e -> loadData());

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });
    }

    // ── Data ─────────────────────────────────────────────────────────────

    private void loadData() {
        model.setRowCount(0);
        try {
            for (AssignedEmployee ae : employeeDAO.getAllEmployees()) {
                model.addRow(new Object[]{
                    ae.getEmployeeId(),
                    ae.getFirstName(),
                    ae.getLastName(),
                    ae.getPhone() == null ? "—" : ae.getPhone(),
                    ae.getYearsOfExperience(),
                    ae.getDateOfBirth(),
                    ae.getGender() == null ? "—" : ae.getGender(),
                    ae.getEventName() == null ? "— Unassigned —" : ae.getEventName()
                });
            }
        } catch (Exception ex) {
            error("Failed to load employees: " + ex.getMessage());
        }
    }

    private void filterTable() {
        String q = tfSearch.getText().trim().toLowerCase();
        model.setRowCount(0);
        try {
            for (AssignedEmployee ae : employeeDAO.getAllEmployees()) {
                boolean match = q.isEmpty()
                    || ae.getFirstName().toLowerCase().contains(q)
                    || ae.getLastName().toLowerCase().contains(q)
                    || ae.getFullName().toLowerCase().contains(q)
                    || (ae.getEventName() != null && ae.getEventName().toLowerCase().contains(q));
                if (match) {
                    model.addRow(new Object[]{
                        ae.getEmployeeId(),
                        ae.getFirstName(),
                        ae.getLastName(),
                        ae.getPhone() == null ? "—" : ae.getPhone(),
                        ae.getYearsOfExperience(),
                        ae.getDateOfBirth(),
                        ae.getGender() == null ? "—" : ae.getGender(),
                        ae.getEventName() == null ? "— Unassigned —" : ae.getEventName()
                    });
                }
            }
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select an employee first."); return; }
        int id = (int) model.getValueAt(row, 0);
        try {
            // Load fresh from DB so the event dropdown is pre-populated correctly
            AssignedEmployee ae = employeeDAO.getEmployeeById(id);
            if (ae != null) showDialog(ae);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select an employee first."); return; }
        int    id   = (int)    model.getValueAt(row, 0);
        String fn   = (String) model.getValueAt(row, 1);
        String ln   = (String) model.getValueAt(row, 2);
        String name = fn + " " + ln;
        int res = JOptionPane.showConfirmDialog(
            this, "Delete employee \"" + name + "\"?\n" +
            "This will also remove their event assignments.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            employeeDAO.deleteEmployee(id);
            loadData();
            if (onDataChanged != null) onDataChanged.onDataChanged();
            JOptionPane.showMessageDialog(this, "Employee deleted.", "Deleted",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    private void showDialog(AssignedEmployee existing) {
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Employee" : "Add New Employee",
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(480, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(formHeader(isEdit ? "Edit Employee" : "New Employee"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(16, 24, 10, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(6,4,6,4);

        JTextField tfFirstName = formField(isEdit ? existing.getFirstName() : "");
        JTextField tfLastName  = formField(isEdit ? existing.getLastName()  : "");
        JTextField tfPhone     = formField(isEdit ? (existing.getPhone() == null ? "" : existing.getPhone()) : "");
        JTextField tfYoe       = formField(isEdit ? String.valueOf(existing.getYearsOfExperience()) : "");
        JTextField tfDob       = formField(isEdit ? existing.getDateOfBirth() : "");
        tfDob.setToolTipText("Format: YYYY-MM-DD  e.g. 1995-08-23");

        JComboBox<String> cbGender = new JComboBox<>(new String[]{"", "Male", "Female"});
        cbGender.setFont(UIConstants.FONT_BODY);
        if (isEdit && existing.getGender() != null) cbGender.setSelectedItem(existing.getGender());

        // Event assignment dropdown — populated from the live events table
        List<Event> events;
        try { events = eventDAO.getAllEvents(); }
        catch (Exception ex) { events = new java.util.ArrayList<>(); }

        JComboBox<String> cbEvent = new JComboBox<>();
        cbEvent.setFont(UIConstants.FONT_BODY);
        cbEvent.addItem("— None / Unassigned —");
        int selIdx = 0;
        for (int i = 0; i < events.size(); i++) {
            Event ev = events.get(i);
            cbEvent.addItem(ev.getEventId() + " | " + ev.getName());
            if (isEdit && ev.getEventId() == existing.getEventId()) selIdx = i + 1;
        }
        cbEvent.setSelectedIndex(selIdx);

        Object[][] rows = {
            {"First Name *",      tfFirstName},
            {"Last Name *",       tfLastName},
            {"Phone",             tfPhone},
            {"Experience (yrs)*", tfYoe},
            {"Date of Birth *",   tfDob},
            {"Gender",            cbGender},
            {"Assigned Event",    cbEvent}
        };
        for (int i = 0; i < rows.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0.4;
            JLabel lbl = new JLabel((String) rows[i][0]);
            lbl.setFont(UIConstants.FONT_BOLD);
            lbl.setForeground(UIConstants.PRIMARY);
            form.add(lbl, gc);
            gc.gridx = 1; gc.weightx = 0.6;
            form.add((Component) rows[i][1], gc);
        }

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(Color.WHITE);
        JButton btnCancel = actionButton("Cancel",   new Color(200,200,200), Color.DARK_GRAY);
        JButton btnSave   = actionButton("  Save  ", UIConstants.SECONDARY,  UIConstants.PRIMARY);
        btns.add(btnCancel);
        btns.add(btnSave);

        root.add(form, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        dlg.setContentPane(root);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            String firstName = tfFirstName.getText().trim();
            String lastName  = tfLastName.getText().trim();
            String phone     = tfPhone.getText().trim();
            String yoeStr    = tfYoe.getText().trim();
            String dobStr    = tfDob.getText().trim();
            String gender    = (String) cbGender.getSelectedItem();

            if (firstName.isEmpty() || lastName.isEmpty() || yoeStr.isEmpty() || dobStr.isEmpty()) {
                error("First name, last name, experience and date of birth are required."); return;
            }
            if (hasDigit(firstName) || hasDigit(lastName)) {
                error("Name fields cannot contain numbers."); return;
            }
            if (!phone.isEmpty() && !isValidPhone(phone)) {
                error("Phone number must contain only digits."); return;
            }
            int yoe;
            try { yoe = Integer.parseInt(yoeStr); }
            catch (NumberFormatException ex) {
                error("Years of experience must be a whole number."); return;
            }
            if (yoe < 0) { error("Years of experience cannot be negative."); return; }
            try { java.time.LocalDate.parse(dobStr); }
            catch (java.time.format.DateTimeParseException ex) {
                error("Date of birth must be in YYYY-MM-DD format  (e.g. 1995-08-23)."); return;
            }

            // Resolve selected event
            int eventId = 0;
            if (cbEvent.getSelectedIndex() > 0) {
                String sel = (String) cbEvent.getSelectedItem();
                eventId = Integer.parseInt(sel.split(" \\| ")[0].trim());
            }

            // Enforce max-3 rule when assigning to a new event
            boolean movingToNewEvent = eventId > 0 &&
                (!isEdit || existing.getEventId() != eventId);
            if (movingToNewEvent) {
                try {
                    if (employeeDAO.countEmployeesForEvent(eventId) >= 3) {
                        error("This event already has 3 employees assigned (maximum)."); return;
                    }
                } catch (Exception ex) { error(ex.getMessage()); return; }
            }

            try {
                AssignedEmployee ae = isEdit ? existing : new AssignedEmployee();
                ae.setFirstName(firstName);
                ae.setLastName(lastName);
                ae.setPhone(phone.isEmpty() ? null : phone);
                ae.setYearsOfExperience(yoe);
                ae.setDateOfBirth(dobStr);
                ae.setGender(gender == null || gender.isEmpty() ? null : gender);
                ae.setEventId(eventId);

                if (isEdit) employeeDAO.updateEmployee(ae);
                else        employeeDAO.addEmployee(ae);

                dlg.dispose();
                loadData();
                if (onDataChanged != null) onDataChanged.onDataChanged();
                JOptionPane.showMessageDialog(this,
                    "Employee " + (isEdit ? "updated" : "added") + " successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                error("Save failed: " + ex.getMessage());
            }
        });

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
                // Highlight "Unassigned" in grey, assigned events in primary colour
                if (col == 7) {
                    String v = val == null ? "" : val.toString();
                    setForeground(sel ? Color.WHITE :
                        v.startsWith("—") ? Color.GRAY : UIConstants.PRIMARY);
                } else {
                    setForeground(sel ? Color.WHITE : Color.BLACK);
                }
                return this;
            }
        });

        // ID | First | Last | Phone | Exp | DOB | Gender | Assigned Event
        int[] widths = {50, 110, 110, 100, 80, 100, 70, 170};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Feature 6: display employee IDs as zero-padded 3-digit strings (001, 002, ...)
        // The model still stores plain int so edit/delete lookups continue to work.
        table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (val instanceof Integer) setText(String.format("%03d", (Integer) val));
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : UIConstants.TABLE_ALT_ROW);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                setForeground(sel ? Color.WHITE : Color.BLACK);
                return this;
            }
        });
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

    private boolean isValidPhone(String phone) { return phone.matches("\\d+"); }
    private boolean hasDigit(String s) { return s.chars().anyMatch(Character::isDigit); }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}
