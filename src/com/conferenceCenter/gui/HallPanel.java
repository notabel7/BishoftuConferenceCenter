package com.conferenceCenter.gui;

import com.conferenceCenter.dao.HallDAO;
import com.conferenceCenter.model.Hall;
import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class HallPanel extends JPanel {

    private final HallDAO dao = new HallDAO();

    private final String[] COLS = {"ID", "Hall Name", "Price / Day (ETB)", "Capacity (Seats)"};
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable     table  = new JTable(model);
    private final JTextField tfSearch = new JTextField(18);

    public HallPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BACKGROUND);
        buildUI();
        loadData();
    }

    private void buildUI() {
        // ── Title bar ────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(UIConstants.PRIMARY);
        titleBar.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("Hall Management");
        title.setFont(UIConstants.FONT_H2);
        title.setForeground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchPanel.setOpaque(false);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(UIConstants.FONT_BOLD);
        searchLbl.setForeground(Color.WHITE);
        tfSearch.setFont(UIConstants.FONT_BODY);
        tfSearch.setPreferredSize(new Dimension(180, 28));
        JButton btnSearch = iconButton();
        applyIcon(btnSearch, "search.png");
        btnSearch.addActionListener(e -> filterTable());
        tfSearch.addActionListener(e -> filterTable());
        tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { filterTable(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { filterTable(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });

        searchPanel.add(searchLbl);
        searchPanel.add(tfSearch);
        searchPanel.add(btnSearch);

        titleBar.add(title,       BorderLayout.WEST);
        titleBar.add(searchPanel, BorderLayout.EAST);

        // ── Table ────────────────────────────────────────────────────
        styleTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(UIConstants.BACKGROUND);
        centerPanel.setBorder(new EmptyBorder(14, 18, 10, 18));
        centerPanel.add(scroll, BorderLayout.CENTER);

        // ── Buttons ──────────────────────────────────────────────────
        JButton btnAdd    = actionButton("Add Hall", new Color(199, 134, 30), Color.WHITE);
        JButton btnEdit   = actionButton("Edit",     new Color(21,101,192), Color.WHITE);
        JButton btnDelete = actionButton("Delete",   UIConstants.DANGER,    Color.WHITE);
        JButton btnRefresh= actionButton("Refresh",  new Color(46,125,50),  Color.WHITE);
        applyIcon(btnAdd,    "add.png");
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

        add(titleBar,   BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(btnPanel,   BorderLayout.SOUTH);

        // ── Actions ──────────────────────────────────────────────────
        btnAdd.addActionListener(e    -> showDialog(null));
        btnEdit.addActionListener(e   -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());
        btnRefresh.addActionListener(e-> loadData());

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
            for (Hall h : dao.getAllHalls()) {
                model.addRow(new Object[]{
                    h.getHallId(), h.getName(),
                    String.format("%.2f", h.getPricePerDay()), h.getCapacity()
                });
            }
        } catch (Exception ex) {
            error("Failed to load halls: " + ex.getMessage());
        }
    }

    private void filterTable() {
        String q = tfSearch.getText().trim().toLowerCase();
        model.setRowCount(0);
        try {
            for (Hall h : dao.getAllHalls()) {
                if (h.getName().toLowerCase().contains(q) || q.isEmpty()) {
                    model.addRow(new Object[]{
                        h.getHallId(), h.getName(),
                        String.format("%.2f", h.getPricePerDay()), h.getCapacity()
                    });
                }
            }
        } catch (Exception ex) {
            error("Filter error: " + ex.getMessage());
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select a hall first."); return; }
        int id = (int) model.getValueAt(row, 0);
        try {
            Hall h = dao.getHallById(id);
            if (h != null) showDialog(h);
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { info("Select a hall first."); return; }
        int id   = (int) model.getValueAt(row, 0);
        String nm = (String) model.getValueAt(row, 1);
        int res = JOptionPane.showConfirmDialog(
            this, "Delete hall \"" + nm + "\"? This cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (res != JOptionPane.YES_OPTION) return;
        try {
            dao.deleteHall(id);
            loadData();
            JOptionPane.showMessageDialog(this, "Hall deleted.", "Deleted",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            error("Cannot delete: " + ex.getMessage());
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────

    private void showDialog(Hall existing) {
        boolean isEdit = (existing != null);
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
            isEdit ? "Edit Hall" : "Add New Hall", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(400, 280);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 24, 10, 24));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(6, 4, 6, 4);

        JTextField tfName     = formField(isEdit ? existing.getName() : "");
        JTextField tfPrice    = formField(isEdit ? String.valueOf(existing.getPricePerDay()) : "");
        JTextField tfCapacity = formField(isEdit ? String.valueOf(existing.getCapacity()) : "");

        Object[][] rows = {
            {"Hall Name *",         tfName},
            {"Price / Day (ETB) *", tfPrice},
            {"Capacity (Seats) *",  tfCapacity}
        };
        for (int i = 0; i < rows.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0.38;
            JLabel lbl = new JLabel((String) rows[i][0]);
            lbl.setFont(UIConstants.FONT_BOLD);
            lbl.setForeground(UIConstants.PRIMARY);
            form.add(lbl, gc);
            gc.gridx = 1; gc.weightx = 0.62;
            form.add((Component) rows[i][1], gc);
        }

        // Buttons
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(Color.WHITE);
        JButton btnSave   = actionButton("  Save  ", UIConstants.SECONDARY, UIConstants.PRIMARY);
        JButton btnCancel = actionButton("Cancel",   new Color(200,200,200), Color.DARK_GRAY);
        btns.add(btnCancel);
        btns.add(btnSave);

        content.add(formHeader(isEdit ? "Edit Hall Details" : "New Hall"), BorderLayout.NORTH);
        content.add(form,  BorderLayout.CENTER);
        content.add(btns,  BorderLayout.SOUTH);
        dlg.setContentPane(content);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnSave.addActionListener(e -> {
            String name = tfName.getText().trim();
            String priceStr = tfPrice.getText().trim();
            String capStr   = tfCapacity.getText().trim();
            if (name.isEmpty() || priceStr.isEmpty() || capStr.isEmpty()) {
                error("All fields are required."); return;
            }
            if (hasDigit(name)) {
                error("Name fields cannot contain numbers."); return;
            }
            double price; int capacity;
            try { price    = Double.parseDouble(priceStr); }
            catch (NumberFormatException ex) { error("Price / Day must be a numeric value (e.g. 2500.00)."); return; }
            try { capacity = Integer.parseInt(capStr); }
            catch (NumberFormatException ex) { error("Capacity must be a whole number (e.g. 200)."); return; }
            if (price <= 0 || capacity <= 0) { error("Price and capacity must be positive numbers."); return; }

            try {
                Hall h = isEdit ? existing : new Hall();
                h.setName(name);
                h.setPricePerDay(price);
                h.setCapacity(capacity);
                if (isEdit) dao.updateHall(h);
                else        dao.addHall(h);
                dlg.dispose();
                loadData();
                JOptionPane.showMessageDialog(this,
                    "Hall " + (isEdit ? "updated" : "added") + " successfully.",
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

        // Custom header renderer — overrides whatever the system L&F injects
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

        // Alternating rows — bold font for consistent, uniform appearance
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : UIConstants.TABLE_ALT_ROW);
                setFont(UIConstants.FONT_BOLD);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
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
        // Natural width so text is never clipped regardless of label length
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setIconTextGap(6);
        return b;
    }

    private void applyIcon(JButton btn, String iconFile) {
        javax.swing.ImageIcon ic = UIConstants.loadIcon(iconFile);
        if (ic != null) btn.setIcon(ic);
    }

    private JButton iconButton() {
        JButton b = new JButton();
        b.setFont(UIConstants.FONT_BODY);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 8, 4, 8));
        return b;
    }

    private boolean hasDigit(String s) {
        return s.chars().anyMatch(Character::isDigit);
    }

    private void info(String msg)  { JOptionPane.showMessageDialog(this, msg, "Info",    JOptionPane.INFORMATION_MESSAGE); }
    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE); }
}
