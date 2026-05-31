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

/**
 * Event management panel.
 *
 * Key design principle (inspired by Cvent / Ungerboeck / EventPro):
 *   Employees and halls are *resources* that exist independently in their own
 *   rosters. An event *selects* from those rosters — it does not create or own
 *   the resources. Deleting an event releases the assignments; the staff and
 *   halls themselves are untouched.
 *
 * Cross-tab integration: after any save/delete, notifies sibling panels via
 * DataChangeListener so the Hall "Status" column and Employee "Assigned Event"
 * column refresh automatically without a manual click.
 */
public class EventPanel extends JPanel {

    private final EventDAO    eventDAO    = new EventDAO();
    private final HallDAO     hallDAO     = new HallDAO();
    private final EmployeeDAO employeeDAO = new EmployeeDAO();

    private final String[] COLS = {
        "ID", "Event Name", "Type", "Start Date", "End Date",
        "Owner", "Halls Booked", "Staff Assigned"
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
                List<AssignedEmployee> staff = employeeDAO.getEmployeesByEvent(ev.getEventId());
                model.addRow(new Object[]{
                    ev.getEventId(), ev.getName(), ev.getType(),
                    ev.getStartDate(), ev.getEndDate(),
                    ev.getOwnerFullName(),          // display-only concatenation, never stored
                    ev.getHallNames(),
                    staff.isEmpty() ? "— None —" : staff.size() + " assigned"
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
                    || ev.getOwnerFullName().toLowerCase().contains(q)
                    || (ev.getStartDate() != null && ev.getStartDate().contains(q))
                    || (ev.getEndDate()   != null && ev.getEndDate().contains(q));
                if (match) {
                    List<AssignedEmployee> staff = employeeDAO.getEmployeesByEvent(ev.getEventId());
                    model.addRow(new Object[]{
                        ev.getEventId(), ev.getName(), ev.getType(),
                        ev.getStartDate(), ev.getEndDate(),
                        ev.getOwnerFullName(),
                        ev.getHallNames(),
                        staff.isEmpty() ? "— None —" : staff.size() + " assigned"
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
            this,
            "Delete event \"" + name + "\"?\n" +
            "Hall bookings and staff assignments will be released.\n" +
            "Staff records themselves will NOT be deleted.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            eventDAO.deleteEvent(id);
            loadData();
            if (onDataChanged != null) onDataChanged.onDataChanged();
            JOptionPane.showMessageDialog(this, "Event deleted.", "Deleted",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    private void viewDetails() {
        int id = selectedEventId();
        if (id < 0) { info("Select an event first."); return; }
        try {
            Event ev = eventDAO.getEventById(id);
            ev.setEmployees(employeeDAO.getEmployeesByEvent(id));
            if (ev != null) showDetailsDialog(ev);
        } catch (Exception ex) { error(ex.getMessage()); }
    }

    // ── Event Add / Edit dialog ───────────────────────────────────────────

    private void showEventDialog(Event existing) {
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Event" : "Add New Event",
            Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(640, 700);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(formHeader(isEdit ? "Edit Event" : "New Event / Conference"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UIConstants.FONT_BOLD);

        // ══════════════════════════════════════════════════════════════
        // TAB 1 — Basic Info  (including dates)
        // ══════════════════════════════════════════════════════════════
        JTextField tfName           = formField(isEdit ? existing.getName()           : "");
        JTextField tfOwnerFirstName = formField(isEdit ? existing.getOwnerFirstName() : "");
        JTextField tfOwnerLastName  = formField(isEdit ? existing.getOwnerLastName()  : "");
        JTextField tfOwnerPhone     = formField(isEdit ? existing.getOwnerPhone()     : "");
        JTextField tfStartDate      = formField(isEdit ? existing.getStartDate()      : "");
        JTextField tfEndDate        = formField(isEdit ? existing.getEndDate()        : "");
        tfStartDate.setToolTipText("Format: YYYY-MM-DD  e.g. 2025-06-15");
        tfEndDate.setToolTipText("Format: YYYY-MM-DD  e.g. 2025-06-17");

        // Feature 3: Date-overlap notification — fires when the user tabs out of End Date.
        // Informational only; does not block saving.
        tfEndDate.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent fe) {
                String sd = tfStartDate.getText().trim();
                String ed = tfEndDate.getText().trim();
                if (!isValidDate(sd) || !isValidDate(ed) || ed.compareTo(sd) < 0) return;
                int excludeId = isEdit ? existing.getEventId() : 0;
                try {
                    java.util.List<String> overlaps =
                        eventDAO.getOverlappingEventNames(sd, ed, excludeId);
                    if (!overlaps.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(overlaps.size()).append(" other event")
                          .append(overlaps.size() > 1 ? "s are" : " is")
                          .append(" already scheduled during these dates:\n\n");
                        for (String n : overlaps) sb.append("  - ").append(n).append("\n");
                        sb.append("\nYou can still proceed if halls have seats available.");
                        JOptionPane.showMessageDialog(dlg, sb.toString(),
                            "Date Overlap Notice", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ignored) {}
            }
        });

        JComboBox<String> cbType = new JComboBox<>(new String[]{
            "Conference", "Seminar", "Workshop", "Wedding", "Birthday",
            "Corporate Meeting", "Training", "Exhibition", "Other"
        });
        if (isEdit) cbType.setSelectedItem(existing.getType());
        cbType.setFont(UIConstants.FONT_BODY);

        JPanel tabBasic = new JPanel(new GridBagLayout());
        tabBasic.setBackground(Color.WHITE);
        tabBasic.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.insets = new Insets(7, 4, 7, 4);

        Object[][] basicRows = {
            {"Event / Conference Name *", tfName},
            {"Event Type *",              cbType},
            {"Start Date * (YYYY-MM-DD)", tfStartDate},
            {"End Date *   (YYYY-MM-DD)", tfEndDate},
            {"Owner First Name *",        tfOwnerFirstName},
            {"Owner Last Name *",         tfOwnerLastName},
            {"Owner Phone *",             tfOwnerPhone}
        };
        for (int i = 0; i < basicRows.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0.40;
            tabBasic.add(fLabel((String) basicRows[i][0]), gc);
            gc.gridx = 1; gc.weightx = 0.60;
            tabBasic.add((Component) basicRows[i][1], gc);
        }

        // ══════════════════════════════════════════════════════════════
        // TAB 2 — Hall Booking  (date-aware, rebuilt each time the tab is shown)
        //
        // Inspired by Function Tracker's "pax count per room" model:
        // each hall shows seats already taken on the chosen dates, the
        // remaining seats, and a spinner to specify how many seats this
        // event needs.  A fully-booked hall is disabled automatically.
        // ══════════════════════════════════════════════════════════════
        final List<JCheckBox> hallChecks = new ArrayList<>();
        final java.util.Map<JCheckBox, JSpinner> hallSpinners = new java.util.HashMap<>();

        // Pre-load existing hall-seat assignments for edit mode
        final java.util.Map<Integer, Integer> existingHallSeats = new java.util.HashMap<>();
        try {
            if (isEdit) existingHallSeats.putAll(hallDAO.getHallSeatsForEvent(existing.getEventId()));
        } catch (Exception ignored) {}

        JPanel hallPickerWrapper = new JPanel();
        hallPickerWrapper.setLayout(new BoxLayout(hallPickerWrapper, BoxLayout.Y_AXIS));
        hallPickerWrapper.setBackground(Color.WHITE);

        JScrollPane hallScroll = new JScrollPane(hallPickerWrapper);
        hallScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));

        JLabel hallFooter = new JLabel(
            "<html><i>Availability is checked against the dates you entered in Basic Info.</i></html>");
        hallFooter.setFont(UIConstants.FONT_SMALL);
        hallFooter.setForeground(new Color(100, 100, 120));
        hallFooter.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel tabHalls = new JPanel(new BorderLayout(0, 8));
        tabHalls.setBackground(Color.WHITE);
        tabHalls.setBorder(new EmptyBorder(12, 20, 12, 20));
        tabHalls.add(fLabel("Select halls and specify seats needed:"), BorderLayout.NORTH);
        tabHalls.add(hallScroll,  BorderLayout.CENTER);
        tabHalls.add(hallFooter,  BorderLayout.SOUTH);

        // Runnable that rebuilds the hall picker using the current dates from Tab 1.
        // Called once at dialog open and again every time the user switches to Tab 2,
        // so date changes in Tab 1 are always reflected here.
        Runnable buildHallPicker = () -> {
            hallPickerWrapper.removeAll();
            hallChecks.clear();
            hallSpinners.clear();

            String sd = tfStartDate.getText().trim();
            String ed = tfEndDate.getText().trim();
            boolean hasDates = isValidDate(sd) && isValidDate(ed) && ed.compareTo(sd) >= 0;
            int excludeId    = isEdit ? existing.getEventId() : 0;

            try {
                List<Hall> allHalls = hallDAO.getAllHalls();
                if (allHalls.isEmpty()) {
                    JLabel none = new JLabel("No halls found. Add halls in the Halls tab first.");
                    none.setFont(UIConstants.FONT_BODY);
                    none.setForeground(Color.GRAY);
                    hallPickerWrapper.add(none);
                } else {
                    for (Hall h : allHalls) {
                        // ── Availability calculation ───────────────────
                        int seatsUsed = 0;
                        if (hasDates) {
                            seatsUsed = hallDAO.getSeatsBookedForHall(
                                h.getHallId(), sd, ed, excludeId);
                        }
                        int seatsLeft = h.getCapacity() - seatsUsed;
                        boolean isFull = hasDates && seatsLeft <= 0;

                        // ── Availability status (inline, colour-coded) ─────
                        // No Unicode symbols — plain ASCII text + color only.
                        String statusMark;
                        Color  statusColor;
                        if (!hasDates) {
                            statusMark  = "   -- enter dates in Basic Info first";
                            statusColor = Color.GRAY;
                        } else if (isFull) {
                            // Feature 4: "Already Taken" label — prominent red text
                            statusMark  = "   [ALREADY TAKEN]  All " + h.getCapacity() + " seats booked";
                            statusColor = UIConstants.DANGER;
                        } else if (seatsUsed > 0) {
                            statusMark  = "   [!]  " + seatsLeft + " of " + h.getCapacity() + " seats still free";
                            statusColor = new Color(230, 81, 0);
                        } else {
                            statusMark  = "   [OK]  All " + h.getCapacity() + " seats available";
                            statusColor = new Color(27, 94, 32);
                        }

                        // Single flat row — no nested vertical panels
                        JPanel hallRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
                        hallRow.setBackground(Color.WHITE);
                        hallRow.setAlignmentX(Component.LEFT_ALIGNMENT);

                        String cbText = h.getName() +
                            "   |   ETB " + String.format("%.2f", h.getPricePerDay()) + "/day" +
                            statusMark;
                        JCheckBox cb = new JCheckBox(cbText);
                        cb.setFont(UIConstants.FONT_BODY);
                        cb.setForeground(statusColor);
                        cb.setBackground(Color.WHITE);
                        cb.putClientProperty("hallId", h.getHallId());
                        cb.setEnabled(!isFull);

                        JLabel seatsLbl = new JLabel("  Seats:");
                        seatsLbl.setFont(UIConstants.FONT_SMALL);
                        seatsLbl.setForeground(UIConstants.PRIMARY);
                        seatsLbl.setVisible(false);

                        // Spinner max = hall capacity (hard ceiling).
                        // A separate warning fires when typed value > available seats.
                        int initVal = 1;
                        if (existingHallSeats.containsKey(h.getHallId()))
                            initVal = Math.min(existingHallSeats.get(h.getHallId()), h.getCapacity());
                        JSpinner spinner = new JSpinner(
                            new SpinnerNumberModel(initVal, 1, h.getCapacity(), 1));
                        spinner.setFont(UIConstants.FONT_BODY);
                        spinner.setPreferredSize(new Dimension(80, 26));
                        spinner.setVisible(false);

                        // Warning when user types a number that exceeds available seats
                        JFormattedTextField spinnerTf =
                            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                        spinnerTf.addFocusListener(new java.awt.event.FocusAdapter() {
                            @Override public void focusLost(java.awt.event.FocusEvent fe) {
                                try {
                                    spinner.commitEdit();
                                    int val = (int) spinner.getValue();
                                    if (hasDates && seatsLeft > 0 && val > seatsLeft) {
                                        JOptionPane.showMessageDialog(dlg,
                                            "\"" + h.getName() + "\" only has " + seatsLeft +
                                            " seats available on your chosen dates\n" +
                                            "(out of " + h.getCapacity() + " total).\n" +
                                            "The value has been corrected to " + seatsLeft + ".",
                                            "Not Enough Seats Available",
                                            JOptionPane.WARNING_MESSAGE);
                                        spinner.setValue(seatsLeft);
                                    }
                                } catch (Exception ignored) {}
                            }
                        });

                        // Pre-select if editing and this hall was already booked
                        if (existingHallSeats.containsKey(h.getHallId()) && !isFull) {
                            cb.setSelected(true);
                            seatsLbl.setVisible(true);
                            spinner.setVisible(true);
                        }

                        cb.addItemListener(ie -> {
                            boolean sel = cb.isSelected();
                            seatsLbl.setVisible(sel);
                            spinner.setVisible(sel);
                            hallRow.revalidate();
                            hallRow.repaint();
                        });

                        hallRow.add(cb);
                        hallRow.add(seatsLbl);
                        hallRow.add(spinner);

                        hallChecks.add(cb);
                        hallSpinners.put(cb, spinner);
                        hallPickerWrapper.add(hallRow);
                    }
                }
            } catch (Exception ex) {
                JLabel err = new JLabel("Error loading halls: " + ex.getMessage());
                err.setForeground(UIConstants.DANGER);
                hallPickerWrapper.add(err);
            }

            hallPickerWrapper.revalidate();
            hallPickerWrapper.repaint();
        };

        // Build immediately so the tab isn't blank on open
        buildHallPicker.run();

        // ══════════════════════════════════════════════════════════════
        // TAB 3 — Staff Assignment
        // ══════════════════════════════════════════════════════════════
        List<AssignedEmployee> allStaff;
        List<Integer> preSelectedEmpIds = new ArrayList<>();
        try {
            allStaff = employeeDAO.getAllEmployeesForPicker();
            if (isEdit) preSelectedEmpIds = employeeDAO.getEmployeeIdsForEvent(existing.getEventId());
        } catch (Exception ex) {
            allStaff = new ArrayList<>();
            error("Could not load staff roster: " + ex.getMessage());
        }

        JPanel staffPickerPanel = new JPanel();
        staffPickerPanel.setBackground(Color.WHITE);
        staffPickerPanel.setLayout(new BoxLayout(staffPickerPanel, BoxLayout.Y_AXIS));
        List<JCheckBox> empChecks = new ArrayList<>();

        if (allStaff.isEmpty()) {
            JLabel noEmp = new JLabel(
                "<html>No employees found in the roster.<br>" +
                "Add staff in the <b>Employees</b> tab, then come back here.</html>");
            noEmp.setFont(UIConstants.FONT_BODY);
            noEmp.setForeground(Color.GRAY);
            noEmp.setBorder(new EmptyBorder(8, 4, 8, 4));
            staffPickerPanel.add(noEmp);
        } else {
            for (AssignedEmployee ae : allStaff) {
                boolean preSelected = preSelectedEmpIds.contains(ae.getEmployeeId());

                // Status mark inline — no separate hint line below.
                // No Unicode symbols — plain ASCII text + color only.
                String statusMark;
                Color  statusColor;
                if (preSelected) {
                    statusMark  = "   [this event]";
                    statusColor = UIConstants.PRIMARY;
                } else if (ae.getEventName() != null) {
                    statusMark  = "   [busy: " + ae.getEventName() + "]";
                    statusColor = new Color(120, 120, 120);
                } else {
                    statusMark  = "   [available]";
                    statusColor = new Color(27, 94, 32);
                }

                String cbLabel = ae.getFirstName() + " " + ae.getLastName() +
                    "   |   " + ae.getYearsOfExperience() + " yr" +
                    (ae.getYearsOfExperience() == 1 ? "" : "s") + " exp" +
                    (ae.getPhone() != null ? "   |   " + ae.getPhone() : "") +
                    statusMark;

                JCheckBox cb = new JCheckBox(cbLabel);
                cb.setFont(UIConstants.FONT_BODY);
                cb.setForeground(statusColor);
                cb.setBackground(Color.WHITE);
                cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                cb.putClientProperty("employeeId", ae.getEmployeeId());
                cb.setSelected(preSelected);

                // Feature 1: one-event-per-employee confirmation + max-3 guard
                cb.addItemListener(ie -> {
                    if (cb.isSelected()) {
                        // Warn if this employee is currently assigned to a different event
                        if (!preSelected && ae.getEventName() != null) {
                            int res = JOptionPane.showConfirmDialog(dlg,
                                ae.getFirstName() + " " + ae.getLastName() +
                                " is currently assigned to \"" + ae.getEventName() + "\".\n" +
                                "Saving will move them here and remove them from that event.\n\n" +
                                "Continue?",
                                "Employee Already Assigned",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                            if (res != JOptionPane.YES_OPTION) {
                                cb.setSelected(false);
                                return;
                            }
                        }
                        // Max-3 guard
                        long checked = empChecks.stream().filter(JCheckBox::isSelected).count();
                        if (checked > 3) {
                            cb.setSelected(false);
                            JOptionPane.showMessageDialog(dlg,
                                "Maximum 3 staff members can be assigned per event.",
                                "Limit Reached", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                });

                empChecks.add(cb);
                staffPickerPanel.add(cb);
            }
        }

        JPanel tabEmployees = new JPanel(new BorderLayout(0, 8));
        tabEmployees.setBackground(Color.WHITE);
        tabEmployees.setBorder(new EmptyBorder(12, 16, 12, 16));
        tabEmployees.add(fLabel("Assign staff from the roster (max 3 per event):"), BorderLayout.NORTH);
        JScrollPane empScroll = new JScrollPane(staffPickerPanel);
        empScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
        tabEmployees.add(empScroll, BorderLayout.CENTER);
        JLabel empHint = new JLabel(
            "<html><i>Staff are managed in the <b>Employees</b> tab. Tick up to 3 to assign.</i></html>");
        empHint.setFont(UIConstants.FONT_SMALL);
        empHint.setForeground(new Color(100, 100, 120));
        empHint.setBorder(new EmptyBorder(4, 0, 0, 0));
        tabEmployees.add(empHint, BorderLayout.SOUTH);

        // ── Assemble tabs ─────────────────────────────────────────────
        tabs.addTab("Basic Info",   tabBasic);
        tabs.addTab("Hall Booking", tabHalls);
        tabs.addTab("Staff",        tabEmployees);

        // Rebuild hall picker each time the user switches to that tab
        // so any date edits in Tab 1 are immediately reflected
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1) buildHallPicker.run();
        });

        // ── Dialog buttons ────────────────────────────────────────────
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(Color.WHITE);
        JButton btnCancel = actionButton("Cancel",   new Color(200,200,200), Color.DARK_GRAY);
        JButton btnSave   = actionButton("  Save  ", UIConstants.SECONDARY,  UIConstants.PRIMARY);
        btns.add(btnCancel);
        btns.add(btnSave);

        root.add(tabs, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        dlg.setContentPane(root);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnSave.addActionListener(e -> {
            // ── Validate Basic Info ───────────────────────────────────
            String name           = tfName.getText().trim();
            String type           = (String) cbType.getSelectedItem();
            String ownerFirstName = tfOwnerFirstName.getText().trim();
            String ownerLastName  = tfOwnerLastName.getText().trim();
            String ownerPhone     = tfOwnerPhone.getText().trim();
            String startDate      = tfStartDate.getText().trim();
            String endDate        = tfEndDate.getText().trim();

            if (name.isEmpty() || ownerFirstName.isEmpty() ||
                    ownerLastName.isEmpty() || ownerPhone.isEmpty() ||
                    startDate.isEmpty() || endDate.isEmpty()) {
                error("All fields including start and end dates are required.");
                tabs.setSelectedIndex(0); return;
            }
            if (hasDigit(name)) {
                error("Event name cannot contain numbers.");
                tabs.setSelectedIndex(0); return;
            }
            if (hasDigit(ownerFirstName) || hasDigit(ownerLastName)) {
                error("Owner name fields cannot contain numbers.");
                tabs.setSelectedIndex(0); return;
            }
            if (!isValidPhone(ownerPhone)) {
                error("Owner phone must contain only digits.");
                tabs.setSelectedIndex(0); return;
            }
            if (!isValidDate(startDate) || !isValidDate(endDate)) {
                error("Dates must be in YYYY-MM-DD format  (e.g. 2025-06-15).");
                tabs.setSelectedIndex(0); return;
            }
            if (endDate.compareTo(startDate) < 0) {
                error("End date cannot be before start date.");
                tabs.setSelectedIndex(0); return;
            }

            // ── Collect selected halls + seat counts ──────────────────
            java.util.Map<Integer,Integer> selectedHallSeats = new java.util.LinkedHashMap<>();
            for (JCheckBox cb : hallChecks) {
                if (cb.isSelected()) {
                    int hallId = (int) cb.getClientProperty("hallId");
                    int seats  = (int) hallSpinners.get(cb).getValue();
                    selectedHallSeats.put(hallId, seats);
                }
            }
            if (selectedHallSeats.isEmpty()) {
                error("Select at least one hall for this event.");
                tabs.setSelectedIndex(1); return;
            }

            // ── Collect selected staff ────────────────────────────────
            List<Integer> selectedEmpIds = new ArrayList<>();
            for (JCheckBox cb : empChecks) {
                if (cb.isSelected()) selectedEmpIds.add((int) cb.getClientProperty("employeeId"));
            }
            if (selectedEmpIds.size() > 3) {
                error("Maximum 3 staff members can be assigned per event.");
                tabs.setSelectedIndex(2); return;
            }

            // Feature 2: soft warning when no staff are assigned
            if (selectedEmpIds.isEmpty()) {
                int proceed = JOptionPane.showConfirmDialog(this,
                    "No staff have been assigned to this event.\n" +
                    "Would you like to save anyway?",
                    "No Staff Assigned",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (proceed != JOptionPane.YES_OPTION) {
                    tabs.setSelectedIndex(2);
                    return;
                }
            }

            try {
                Event ev = isEdit ? existing : new Event();
                ev.setName(name);
                ev.setType(type);
                ev.setOwnerFirstName(ownerFirstName);
                ev.setOwnerLastName(ownerLastName);
                ev.setOwnerPhone(ownerPhone);
                ev.setStartDate(startDate);
                ev.setEndDate(endDate);

                int eventId;
                if (isEdit) {
                    eventDAO.updateEvent(ev, selectedHallSeats);
                    eventId = existing.getEventId();
                } else {
                    eventId = eventDAO.addEvent(ev, selectedHallSeats);
                }

                employeeDAO.setEventEmployees(eventId, selectedEmpIds);

                dlg.dispose();
                loadData();
                if (onDataChanged != null) onDataChanged.onDataChanged();
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
        dlg.setSize(580, 540);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(formHeader("Event Details"), BorderLayout.NORTH);

        // ── Content panel (GridBagLayout for clean key-value alignment) ──
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(18, 28, 18, 28));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill   = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        int y = 0;

        // ── Section: Event Information ────────────────────────────────
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
        gc.weightx = 1.0; gc.insets = new Insets(0, 0, 6, 0);
        content.add(detailSectionHeader("Event Information"), gc);

        String[][] fields = {
            {"Event Name",  ev.getName()},
            {"Type",        ev.getType()},
            {"Dates",       ev.getStartDate() + "   →   " + ev.getEndDate()},
            {"Owner",       ev.getOwnerFullName()},
            {"Owner Phone", ev.getOwnerPhone()}
        };
        gc.gridwidth = 1; gc.insets = new Insets(3, 8, 3, 8);
        for (String[] row : fields) {
            gc.gridx = 0; gc.gridy = y; gc.weightx = 0.30;
            content.add(detailKey(row[0]), gc);
            gc.gridx = 1; gc.weightx = 0.70;
            content.add(detailVal(row[1]), gc);
            y++;
        }

        // ── Gap ───────────────────────────────────────────────────────
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
        gc.insets = new Insets(10, 0, 0, 0);
        content.add(new JSeparator(), gc);

        // ── Section: Halls Booked ─────────────────────────────────────
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
        gc.insets = new Insets(10, 0, 6, 0);
        content.add(detailSectionHeader("Halls Booked  (" + ev.getHalls().size() + ")"), gc);

        gc.insets = new Insets(3, 8, 3, 0);
        if (ev.getHalls().isEmpty()) {
            gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
            content.add(detailEmpty("No halls booked."), gc);
        } else {
            for (Hall h : ev.getHalls()) {
                // Each hall is ONE line: name | seats reserved | total capacity | price
                String line = h.getName()
                    + "     " + h.getSeatsRequested() + " seats reserved"
                    + "  (of " + h.getCapacity() + " total)"
                    + "     ETB " + String.format("%.2f", h.getPricePerDay()) + " / day";
                gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2; gc.weightx = 1.0;
                content.add(detailEntry(">", line, UIConstants.PRIMARY), gc);
            }
        }

        // ── Gap ───────────────────────────────────────────────────────
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
        gc.insets = new Insets(10, 0, 0, 0);
        content.add(new JSeparator(), gc);

        // ── Section: Assigned Staff ───────────────────────────────────
        gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
        gc.insets = new Insets(10, 0, 6, 0);
        content.add(detailSectionHeader("Assigned Staff  (" + ev.getEmployees().size() + ")"), gc);

        gc.insets = new Insets(3, 8, 3, 0);
        if (ev.getEmployees().isEmpty()) {
            gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
            content.add(detailEmpty("No staff assigned."), gc);
        } else {
            for (AssignedEmployee ae : ev.getEmployees()) {
                // Each staff member is ONE line — all fields including gender
                String line = ae.getFullName()
                    + "     " + ae.getYearsOfExperience() + " yr"
                    + (ae.getYearsOfExperience() == 1 ? "" : "s") + " exp"
                    + "     DOB: " + ae.getDateOfBirth()
                    + (ae.getGender() != null ? "     " + ae.getGender() : "")
                    + (ae.getPhone() != null  ? "     Phone: " + ae.getPhone() : "");
                gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2; gc.weightx = 1.0;
                content.add(detailEntry(">", line, Color.BLACK), gc);
            }
        }

        // Vertical filler so the content doesn't float in the middle
        gc.gridx = 0; gc.gridy = y; gc.gridwidth = 2;
        gc.weighty = 1.0; gc.insets = new Insets(0, 0, 0, 0);
        content.add(new JPanel() {{ setOpaque(false); }}, gc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btns.setBackground(Color.WHITE);
        btns.setBorder(new EmptyBorder(0, 0, 10, 0));
        JButton close = actionButton("  Close  ", UIConstants.PRIMARY, Color.WHITE);
        close.addActionListener(e -> dlg.dispose());
        btns.add(close);

        root.add(scroll, BorderLayout.CENTER);
        root.add(btns,   BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ── Details dialog helpers ────────────────────────────────────────────

    /** Bold primary-coloured section title with a separator line beneath it. */
    private JPanel detailSectionHeader(String text) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BOLD);
        lbl.setForeground(UIConstants.PRIMARY);
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_COLOR);
        p.add(lbl, BorderLayout.NORTH);
        p.add(sep, BorderLayout.SOUTH);
        return p;
    }

    /** Key label (right-aligned, muted). */
    private JLabel detailKey(String text) {
        JLabel l = new JLabel(text + ":");
        l.setFont(UIConstants.FONT_BOLD);
        l.setForeground(new Color(90, 90, 90));
        return l;
    }

    /** Value label (left-aligned, black). */
    private JLabel detailVal(String text) {
        JLabel l = new JLabel(text == null || text.isEmpty() ? "—" : text);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(Color.BLACK);
        return l;
    }

    /** Single-line entry for halls and staff (bullet + text). */
    private JPanel detailEntry(String bullet, String text, Color textColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        p.setBackground(Color.WHITE);
        JLabel blt = new JLabel(bullet);
        blt.setFont(UIConstants.FONT_BOLD);
        blt.setForeground(UIConstants.SECONDARY);
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(textColor);
        p.add(blt);
        p.add(lbl);
        return p;
    }

    /** Shown when a section has no items. */
    private JLabel detailEmpty(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BODY);
        l.setForeground(Color.GRAY);
        return l;
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
                if (!sel) setBackground(UIConstants.TABLE_ALT_ROW);
                setFont(UIConstants.FONT_BOLD);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                // Colour-code the Staff and Halls columns; everything else: solid black
                if (!sel) {
                    if (col == 7) {
                        String v = val == null ? "" : val.toString();
                        setForeground(v.startsWith("—") ? Color.GRAY : UIConstants.PRIMARY);
                    } else if (col == 6) {
                        String v = val == null ? "" : val.toString();
                        setForeground(v.startsWith("—") ? Color.GRAY : UIConstants.PRIMARY);
                    } else {
                        setForeground(Color.BLACK);
                    }
                } else {
                    setForeground(Color.WHITE);
                }
                return this;
            }
        });

        // ID | Name | Type | Start | End | Owner | Halls | Staff
        int[] widths = {40, 160, 100, 95, 95, 140, 150, 90};
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

    private boolean isValidPhone(String phone) { return phone.matches("\\d+"); }
    private boolean hasDigit(String s)         { return s.chars().anyMatch(Character::isDigit); }
    private boolean isValidDate(String d) {
        if (d == null || d.length() != 10) return false;
        try { java.time.LocalDate.parse(d); return true; }
        catch (java.time.format.DateTimeParseException e) { return false; }
    }
    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",  JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
}
