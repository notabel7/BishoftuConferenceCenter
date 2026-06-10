package com.conferenceCenter.gui;

import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * Abstract base class for the three main CRUD panels (Hall, Event, Employee).
 *
 * Demonstrates GUI-layer inheritance: the shared UI factory helpers and dialog
 * shortcuts live here exactly once, so subclasses stay focused on their own
 * domain logic instead of re-declaring identical boilerplate.
 *
 * Subclasses must implement {@link #refresh()}, which MainFrame's cross-tab
 * wiring (DataChangeListener) calls so sibling panels stay in sync after any
 * data mutation.
 */
public abstract class BaseCrudPanel extends JPanel {

    protected BaseCrudPanel() {
        setBackground(UIConstants.BACKGROUND);
    }

    // ── Shared UI factory methods ─────────────────────────────────────────

    /** Solid primary-coloured header strip used at the top of every dialog. */
    protected JPanel formHeader(String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 10));
        p.setBackground(UIConstants.PRIMARY);
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_H2);
        l.setForeground(Color.WHITE);
        p.add(l);
        return p;
    }

    /** Styled single-line text field used in every add/edit form. */
    protected JTextField formField(String val) {
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

    /** Flat coloured action button used across all panels and dialogs. */
    protected JButton actionButton(String text, Color bg, Color fg) {
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

    /** Attaches an icon from resources/icons to a button, if it exists. */
    protected void applyIcon(JButton btn, String iconFile) {
        ImageIcon ic = UIConstants.loadIcon(iconFile);
        if (ic != null) btn.setIcon(ic);
    }

    /**
     * Applies the table styling shared by every panel: fonts, row height,
     * selection colours, and the primary-coloured column-header renderer.
     * Subclasses then layer their own cell renderers and column widths on top.
     */
    protected void styleTableBase(JTable table) {
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
    }

    // ── Shared dialog helpers ─────────────────────────────────────────────

    protected void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── Abstract contract ─────────────────────────────────────────────────

    /** Reload this panel's table from the database. Called by cross-tab wiring. */
    public abstract void refresh();
}
