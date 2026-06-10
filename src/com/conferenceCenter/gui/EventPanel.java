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
public class EventPanel extends BaseCrudPanel {

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
    @Override
    public void refresh() { loadData(); }

    public EventPanel() {
        // Background colour is set by the BaseCrudPanel constructor.
        setLayout(new BorderLayout());
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

    /** Builds one table row for an event (looks up its staff count) — single source of truth. */
    private void addRowFor(Event ev) throws java.sql.SQLException {
        List<AssignedEmployee> staff = employeeDAO.getEmployeesByEvent(ev.getEventId());
        model.addRow(new Object[]{
            ev.getEventId(), ev.getName(), ev.getType(),
            ev.getStartDate(), ev.getEndDate(),
            ev.getOwnerFullName(),          // display-only concatenation, never stored
            ev.getHallNames(),
            staff.isEmpty() ? "— None —" : staff.size() + " assigned"
        });
    }

    private void loadData() {
        model.setRowCount(0);
        try {
            for (Event ev : eventDAO.getAllEvents()) addRowFor(ev);
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
                if (match) addRowFor(ev);
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
            if (ev == null) { info("Event no longer exists."); return; }
            // getEventById already populates halls + employees internally
            showDetailsDialog(ev);
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
        JTextField tfStartDate      = hintField(isEdit ? existing.getStartDate() : "", "e.g.  2025-06-15");
        JTextField tfEndDate        = hintField(isEdit ? existing.getEndDate()   : "", "e.g.  2025-06-17");

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
                } catch (Exception ignored) {
                    // Non-fatal: the overlap notice is a courtesy hint, not a gate on saving.
                    System.err.println("[EventPanel] date-overlap check failed: " + ignored.getMessage());
                }
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
        tabBasic.setBorder(new EmptyBorder(10, 20, 10, 20));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Helper to add a full-width section header spanning both columns
        int bRow = 0;

        // ── Section: Event Details ────────────────────────────────────
        gc.gridx = 0; gc.gridy = bRow; gc.gridwidth = 2; gc.weightx = 1.0;
        gc.insets = new Insets(4, 4, 4, 4);
        tabBasic.add(sectionHeader("Event Details"), gc);
        gc.gridwidth = 1; bRow++;

        Object[][] eventRows = {
            {"Name *", tfName},
            {"Type *", cbType}
        };
        for (Object[] r : eventRows) {
            gc.insets = new Insets(3, 4, 3, 4);
            gc.gridx = 0; gc.gridy = bRow; gc.weightx = 0.38;
            tabBasic.add(fLabel((String) r[0]), gc);
            gc.gridx = 1; gc.weightx = 0.62;
            tabBasic.add((Component) r[1], gc);
            bRow++;
        }

        // ── Section: Dates ────────────────────────────────────────────
        gc.gridx = 0; gc.gridy = bRow; gc.gridwidth = 2; gc.weightx = 1.0;
        gc.insets = new Insets(12, 4, 4, 4);
        tabBasic.add(sectionHeader("Event Dates"), gc);
        gc.gridwidth = 1; bRow++;

        Object[][] dateRows = {
            {"Start Date *", tfStartDate},
            {"End Date *",   tfEndDate}
        };
        for (Object[] r : dateRows) {
            gc.insets = new Insets(3, 4, 3, 4);
            gc.gridx = 0; gc.gridy = bRow; gc.weightx = 0.38;
            tabBasic.add(fLabel((String) r[0]), gc);
            gc.gridx = 1; gc.weightx = 0.62;
            tabBasic.add((Component) r[1], gc);
            bRow++;
        }

        // ── Section: Owner Contact ────────────────────────────────────
        gc.gridx = 0; gc.gridy = bRow; gc.gridwidth = 2; gc.weightx = 1.0;
        gc.insets = new Insets(12, 4, 4, 4);
        tabBasic.add(sectionHeader("Owner Contact"), gc);
        gc.gridwidth = 1; bRow++;

        Object[][] ownerRows = {
            {"First Name *", tfOwnerFirstName},
            {"Last Name *",  tfOwnerLastName},
            {"Phone *",      tfOwnerPhone}
        };
        for (Object[] r : ownerRows) {
            gc.insets = new Insets(3, 4, 3, 4);
            gc.gridx = 0; gc.gridy = bRow; gc.weightx = 0.38;
            tabBasic.add(fLabel((String) r[0]), gc);
            gc.gridx = 1; gc.weightx = 0.62;
            tabBasic.add((Component) r[1], gc);
            bRow++;
        }

        // Push everything to the top
        gc.gridx = 0; gc.gridy = bRow; gc.gridwidth = 2;
        gc.weighty = 1.0; gc.fill = GridBagConstraints.BOTH;
        tabBasic.add(new JPanel() {{ setOpaque(false); }}, gc);

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
        } catch (Exception ignored) {
            // Non-fatal: if preloading fails, the seat spinners just start at their default.
            System.err.println("[EventPanel] could not preload hall seats: " + ignored.getMessage());
        }

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

                        // ── Availability status text — clean, no bracket tags ──
                        String statusText;
                        Color  statusColor;
                        if (!hasDates) {
                            statusText  = "Enter dates in Basic Info first";
                            statusColor = new Color(150, 150, 150);
                        } else if (isFull) {
                            statusText  = "ALREADY TAKEN  (all " + h.getCapacity() + " seats booked)";
                            statusColor = UIConstants.DANGER;
                        } else if (seatsUsed > 0) {
                            statusText  = seatsLeft + " of " + h.getCapacity() + " seats free";
                            statusColor = new Color(230, 81, 0);
                        } else {
                            statusText  = "All " + h.getCapacity() + " seats available";
                            statusColor = new Color(27, 94, 32);
                        }

                        // ── Row: tight single line, thin bottom divider ────
                        JPanel hallRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
                        hallRow.setBackground(Color.WHITE);
                        hallRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                        hallRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                        hallRow.setBorder(BorderFactory.createMatteBorder(
                            0, 0, 1, 0, new Color(220, 220, 235)));

                        // Hall name — bold, black (or grey if taken)
                        JCheckBox cb = new JCheckBox(h.getName());
                        cb.setFont(UIConstants.FONT_BOLD);
                        cb.setForeground(isFull ? new Color(160, 160, 160) : Color.BLACK);
                        cb.setBackground(Color.WHITE);
                        cb.putClientProperty("hallId", h.getHallId());
                        cb.setEnabled(!isFull);

                        // Price — small, muted grey
                        JLabel priceLbl = new JLabel(
                            "ETB " + String.format("%.0f", h.getPricePerDay()) + "/day");
                        priceLbl.setFont(UIConstants.FONT_SMALL);
                        priceLbl.setForeground(new Color(110, 110, 110));

                        // Divider pip
                        JLabel pipLbl = new JLabel("|");
                        pipLbl.setFont(UIConstants.FONT_SMALL);
                        pipLbl.setForeground(new Color(190, 190, 200));

                        // Status — small, color-coded, no brackets
                        JLabel statusLbl = new JLabel(statusText);
                        statusLbl.setFont(isFull
                            ? new Font(UIConstants.FONT_SMALL.getName(), Font.BOLD,
                                       UIConstants.FONT_SMALL.getSize())
                            : UIConstants.FONT_SMALL);
                        statusLbl.setForeground(statusColor);

                        // Seats label — shown only when checkbox is ticked
                        String seatsLblText = hasDates
                            ? "   Seats (max " + seatsLeft + "):"
                            : "   Seats:";
                        JLabel seatsLbl = new JLabel(seatsLblText);
                        seatsLbl.setFont(UIConstants.FONT_SMALL);
                        seatsLbl.setForeground(UIConstants.PRIMARY);
                        seatsLbl.setVisible(false);

                        // Spinner max = seatsLeft (remaining available seats), NOT full capacity.
                        // This means arrow-clicking is also hard-capped at what's actually free.
                        // Final copies needed for capture inside the FocusAdapter lambda.
                        final int seatsUsedFinal = seatsUsed;
                        int spinnerMax = Math.max(1, seatsLeft); // always at least 1 to avoid model error
                        int initVal = 1;
                        if (existingHallSeats.containsKey(h.getHallId()))
                            initVal = Math.min(existingHallSeats.get(h.getHallId()), spinnerMax);
                        JSpinner spinner = new JSpinner(
                            new SpinnerNumberModel(initVal, 1, spinnerMax, 1));
                        spinner.setFont(UIConstants.FONT_BODY);
                        spinner.setPreferredSize(new Dimension(80, 26));
                        spinner.setVisible(false);

                        // Warning when user types a number that exceeds available seats.
                        // IMPORTANT: read raw text BEFORE commitEdit() — commitEdit silently
                        // clamps the value to the model max, so checking after it always passes.
                        JFormattedTextField spinnerTf =
                            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
                        spinnerTf.addFocusListener(new java.awt.event.FocusAdapter() {
                            @Override public void focusLost(java.awt.event.FocusEvent fe) {
                                try {
                                    int typedVal = Integer.parseInt(spinnerTf.getText().trim());
                                    if (typedVal > spinnerMax) {
                                        JOptionPane.showMessageDialog(dlg,
                                            "Not enough seats available in \"" + h.getName() + "\".\n\n" +
                                            "  Total capacity : " + h.getCapacity() + " seats\n" +
                                            "  Already booked : " + seatsUsedFinal + " seats\n" +
                                            "  Remaining      : " + seatsLeft + " seats\n\n" +
                                            "You entered " + typedVal + ". Maximum you can book is " + spinnerMax + ".",
                                            "Not Enough Seats Available",
                                            JOptionPane.WARNING_MESSAGE);
                                        spinner.setValue(spinnerMax);
                                        return;
                                    }
                                } catch (NumberFormatException ignored) {
                                    // Field empty or non-numeric — let commitEdit normalise it below.
                                }
                                try {
                                    spinner.commitEdit();
                                } catch (Exception ignored) {
                                    // Invalid partial text — keep the spinner's last valid value.
                                }
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
                        hallRow.add(priceLbl);
                        hallRow.add(pipLbl);
                        hallRow.add(statusLbl);
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

        // Effectively-final copy for use in the save lambda
        final List<Integer> preSelectedFinal = preSelectedEmpIds;
        // Lookup maps (built below) for the save-time "move" confirmation.
        // empId → full name, and empId → the event they're CURRENTLY assigned to.
        final java.util.Map<Integer,String> staffName        = new java.util.HashMap<>();
        final java.util.Map<Integer,String> staffCurrentEvent = new java.util.HashMap<>();

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

                // Record name + current assignment for the save-time move check
                staffName.put(ae.getEmployeeId(), ae.getFirstName() + " " + ae.getLastName());
                if (ae.getEventName() != null)
                    staffCurrentEvent.put(ae.getEmployeeId(), ae.getEventName());

                // ── Status text — clean, no brackets ──────────────────
                String statusText;
                Color  statusColor;
                if (preSelected) {
                    statusText  = "Assigned to this event";
                    statusColor = UIConstants.PRIMARY;
                } else if (ae.getEventName() != null) {
                    statusText  = "Busy: " + ae.getEventName();
                    statusColor = new Color(150, 150, 150);
                } else {
                    statusText  = "Available";
                    statusColor = new Color(27, 94, 32);
                }

                // ── Row: tight single line, thin bottom divider ────────
                JPanel empRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
                empRow.setBackground(Color.WHITE);
                empRow.setAlignmentX(Component.LEFT_ALIGNMENT);
                empRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
                empRow.setBorder(BorderFactory.createMatteBorder(
                    0, 0, 1, 0, new Color(220, 220, 235)));

                // Name — bold, black (primary identifier)
                JCheckBox cb = new JCheckBox(
                    ae.getFirstName() + " " + ae.getLastName());
                cb.setFont(UIConstants.FONT_BOLD);
                cb.setForeground(Color.BLACK);
                cb.setBackground(Color.WHITE);
                cb.putClientProperty("employeeId", ae.getEmployeeId());
                cb.setSelected(preSelected);

                // Experience — small, muted
                JLabel expLbl = new JLabel(
                    ae.getYearsOfExperience() + " yr" +
                    (ae.getYearsOfExperience() == 1 ? "" : "s") + " exp");
                expLbl.setFont(UIConstants.FONT_SMALL);
                expLbl.setForeground(new Color(110, 110, 110));

                // Pip separator
                JLabel pip1 = new JLabel("|");
                pip1.setFont(UIConstants.FONT_SMALL);
                pip1.setForeground(new Color(190, 190, 200));

                // Phone — small, muted (only if present)
                JLabel phoneLbl = new JLabel(
                    ae.getPhone() != null ? ae.getPhone() : "");
                phoneLbl.setFont(UIConstants.FONT_SMALL);
                phoneLbl.setForeground(new Color(110, 110, 110));
                phoneLbl.setVisible(ae.getPhone() != null);

                JLabel pip2 = new JLabel("|");
                pip2.setFont(UIConstants.FONT_SMALL);
                pip2.setForeground(new Color(190, 190, 200));
                pip2.setVisible(ae.getPhone() != null);

                // Status — small, color-coded, no brackets
                JLabel statusLbl = new JLabel(statusText);
                statusLbl.setFont(preSelected
                    ? new java.awt.Font(UIConstants.FONT_SMALL.getName(),
                                        java.awt.Font.BOLD,
                                        UIConstants.FONT_SMALL.getSize())
                    : UIConstants.FONT_SMALL);
                statusLbl.setForeground(statusColor);

                // NOTE: No modal dialogs are shown from here. Confirming an
                // employee's reassignment (Feature 1) and the max-3 limit are both
                // validated at SAVE time. Showing a modal JOptionPane from inside
                // an ItemListener causes the checkbox's pending click to reprocess
                // when the dialog closes, silently un-ticking the box — which made
                // "confirm the move, then nothing moves" happen.

                empRow.add(cb);
                empRow.add(expLbl);
                empRow.add(pip1);
                empRow.add(phoneLbl);
                empRow.add(pip2);
                empRow.add(statusLbl);

                empChecks.add(cb);
                staffPickerPanel.add(empRow);
            }
        }

        JPanel tabEmployees = new JPanel(new BorderLayout(0, 8));
        tabEmployees.setBackground(Color.WHITE);
        tabEmployees.setBorder(new EmptyBorder(12, 16, 12, 16));
        tabEmployees.add(fLabel("Assign staff from the roster (max " +
            Event.MAX_STAFF_PER_EVENT + " per event):"), BorderLayout.NORTH);
        JScrollPane empScroll = new JScrollPane(staffPickerPanel);
        empScroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
        tabEmployees.add(empScroll, BorderLayout.CENTER);
        JLabel empHint = new JLabel(
            "<html><i>Staff are managed in the <b>Employees</b> tab. Tick up to " +
            Event.MAX_STAFF_PER_EVENT + " to assign.</i></html>");
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

            // Hard seat-availability check at save time — catches any case where
            // the spinner warning was bypassed (e.g. picker built before dates were entered).
            int excludeIdFinal = isEdit ? existing.getEventId() : 0;
            for (java.util.Map.Entry<Integer,Integer> entry : selectedHallSeats.entrySet()) {
                try {
                    int    hid       = entry.getKey();
                    int    requested = entry.getValue();
                    int    booked    = hallDAO.getSeatsBookedForHall(hid, startDate, endDate, excludeIdFinal);
                    Hall   hall      = hallDAO.getHallById(hid);
                    int    available = hall.getCapacity() - booked;
                    if (requested > available) {
                        error("Not enough seats in \"" + hall.getName() + "\".\n\n" +
                              "  Total capacity : " + hall.getCapacity() + " seats\n" +
                              "  Already booked : " + booked + " seats\n" +
                              "  Remaining      : " + available + " seats\n\n" +
                              "You requested " + requested + ". Please reduce the seat count.");
                        tabs.setSelectedIndex(1);
                        return;
                    }
                } catch (java.sql.SQLException ex) {
                    error("Seat validation failed: " + ex.getMessage()); return;
                }
            }

            // ── Collect selected staff ────────────────────────────────
            List<Integer> selectedEmpIds = new ArrayList<>();
            for (JCheckBox cb : empChecks) {
                if (cb.isSelected()) selectedEmpIds.add((int) cb.getClientProperty("employeeId"));
            }
            if (selectedEmpIds.size() > Event.MAX_STAFF_PER_EVENT) {
                error("Maximum " + Event.MAX_STAFF_PER_EVENT +
                      " staff members can be assigned per event.");
                tabs.setSelectedIndex(2); return;
            }

            // Feature 1: confirm reassignment of any staff currently on another event.
            // Done here (not in the checkbox listener) so the selection is reliable.
            List<String> movers = new ArrayList<>();
            for (int id : selectedEmpIds) {
                if (!preSelectedFinal.contains(id) && staffCurrentEvent.containsKey(id)) {
                    movers.add("   - " + staffName.get(id) +
                               "  (currently on \"" + staffCurrentEvent.get(id) + "\")");
                }
            }
            if (!movers.isEmpty()) {
                int proceed = JOptionPane.showConfirmDialog(this,
                    "The following staff will be moved to this event and removed\n" +
                    "from the event they are currently assigned to:\n\n" +
                    String.join("\n", movers) + "\n\n" +
                    "Continue?",
                    "Reassign Staff",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (proceed != JOptionPane.YES_OPTION) {
                    tabs.setSelectedIndex(2);
                    return;
                }
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
            // Calculate event duration for cost estimate
            long days = 1;
            try {
                java.time.LocalDate sd = java.time.LocalDate.parse(ev.getStartDate());
                java.time.LocalDate ed = java.time.LocalDate.parse(ev.getEndDate());
                days = java.time.temporal.ChronoUnit.DAYS.between(sd, ed) + 1;
            } catch (Exception ignored) {
                // Unparseable dates — fall back to the 1-day default already set above.
            }

            double totalCost = 0;
            for (Hall h : ev.getHalls()) {
                totalCost += h.getPricePerDay() * days;
                String line = h.getName()
                    + "     " + h.getSeatsRequested() + " seats reserved"
                    + "  (of " + h.getCapacity() + " total)"
                    + "     ETB " + String.format("%,.2f", h.getPricePerDay()) + " / day";
                gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2; gc.weightx = 1.0;
                content.add(detailEntry("—", line, UIConstants.PRIMARY), gc);
            }

            // Estimated total cost — highlighted row
            final long durationDays = days;
            final double finalCost  = totalCost;
            gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2;
            gc.insets = new Insets(8, 8, 4, 8);
            JPanel costPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            costPanel.setBackground(new Color(232, 240, 254));
            costPanel.setBorder(BorderFactory.createMatteBorder(
                1, 3, 1, 0, UIConstants.PRIMARY));
            JLabel costKey = new JLabel("Estimated Total:");
            costKey.setFont(UIConstants.FONT_BOLD);
            costKey.setForeground(new Color(80, 80, 90));
            JLabel costVal = new JLabel(
                "ETB " + String.format("%,.2f", finalCost) +
                "   (" + durationDays + " day" + (durationDays == 1 ? "" : "s") + ")");
            costVal.setFont(new java.awt.Font(
                UIConstants.FONT_BOLD.getName(), java.awt.Font.BOLD, 14));
            costVal.setForeground(UIConstants.PRIMARY);
            costPanel.add(costKey);
            costPanel.add(costVal);
            content.add(costPanel, gc);
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
                // Event context: name, experience, phone — DOB/gender are HR data, not relevant here
                String line = ae.getFullName()
                    + "     " + ae.getYearsOfExperience() + " yr"
                    + (ae.getYearsOfExperience() == 1 ? "" : "s") + " exp"
                    + (ae.getPhone() != null ? "     Phone: " + ae.getPhone() : "");
                gc.gridx = 0; gc.gridy = y++; gc.gridwidth = 2; gc.weightx = 1.0;
                content.add(detailEntry("—", line, Color.BLACK), gc);
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

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 8));
        btns.setBackground(Color.WHITE);
        btns.setBorder(new EmptyBorder(0, 0, 4, 8));
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
        styleTableBase(table);   // shared setup + header renderer (BaseCrudPanel)

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

    // formHeader, formField, actionButton, applyIcon, info, error
    // are inherited from BaseCrudPanel.

    /** Text field that shows grey italic placeholder text when empty and unfocused. */
    private JTextField hintField(String val, String hint) {
        JTextField f = new JTextField(val) {
            @Override protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(new Color(180, 180, 190));
                    g2.setFont(getFont().deriveFont(java.awt.Font.ITALIC));
                    java.awt.Insets ins = getInsets();
                    java.awt.FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(hint, ins.left + 2,
                        ins.top + (getHeight() - ins.top - ins.bottom + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            }
        };
        f.setFont(UIConstants.FONT_BODY);
        f.setBackground(UIConstants.INPUT_BG);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        f.setPreferredSize(new Dimension(0, UIConstants.FIELD_H));
        // Repaint on focus change so placeholder appears/disappears cleanly
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) { f.repaint(); }
            @Override public void focusLost(java.awt.event.FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    /** Bold section header with a thin horizontal rule — used in the Basic Info form. */
    private JPanel sectionHeader(String title) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(title);
        lbl.setFont(new java.awt.Font(UIConstants.FONT_BOLD.getName(),
                                      java.awt.Font.BOLD, 11));
        lbl.setForeground(UIConstants.PRIMARY);
        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        sep.setForeground(new Color(200, 205, 230));
        p.add(lbl, BorderLayout.WEST);
        p.add(sep, BorderLayout.CENTER);
        return p;
    }

    private JLabel fLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BOLD);
        l.setForeground(UIConstants.PRIMARY);
        return l;
    }

    private boolean isValidPhone(String phone) { return phone.matches("\\d+"); }
    private boolean hasDigit(String s)         { return s.chars().anyMatch(Character::isDigit); }
    private boolean isValidDate(String d) {
        if (d == null || d.length() != 10) return false;
        try { java.time.LocalDate.parse(d); return true; }
        catch (java.time.format.DateTimeParseException e) { return false; }
    }
}
