package com.conferenceCenter.gui;

import com.conferenceCenter.dao.AdminDAO;
import com.conferenceCenter.model.Admin;
import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MainFrame extends JFrame {

    private final Admin              loggedIn;
    private final JTabbedPane        tabs      = new JTabbedPane(JTabbedPane.LEFT);
    private final JLabel             lblUser;
    private final java.util.List<JLabel> tabLabels = new java.util.ArrayList<>();

    public MainFrame(Admin admin) {
        this.loggedIn = admin;
        lblUser = new JLabel("  Logged in as: " + admin.getFirstName() + " " + admin.getLastName() + "  ");

        setTitle("Bishoftu Conference Center  Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Application window icon (replaces the default Java coffee-cup)
        javax.swing.ImageIcon appLogo = UIConstants.loadIcon("app_logo.png", 32, 32);
        if (appLogo != null) setIconImage(appLogo.getImage());

        buildUI();
        buildMenu();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UIConstants.BACKGROUND);
        setContentPane(root);

        // ── Top header ────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(
                    0, 0, UIConstants.PRIMARY,
                    getWidth(), 0, new Color(48, 63, 159)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setPreferredSize(new Dimension(0, 62));
        header.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel iconLbl = new JLabel("Bishoftu Conference Center");
        iconLbl.setFont(UIConstants.FONT_H2);
        iconLbl.setForeground(Color.WHITE);

        lblUser.setFont(UIConstants.FONT_SMALL);
        lblUser.setForeground(UIConstants.SECONDARY);

        header.add(iconLbl,  BorderLayout.WEST);
        header.add(lblUser,  BorderLayout.EAST);

        // ── Tabs ──────────────────────────────────────────────────────
        // Sidebar has a dark-blue background; each tab uses white icons + white text.
        tabs.setBackground(UIConstants.PRIMARY);
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        addStyledTab("Halls",     new HallPanel(),     "halls.png");
        addStyledTab("Events",    new EventPanel(),    "events.png");
        addStyledTab("Employees", new EmployeePanel(), "employees.png");

        tabs.addChangeListener(e -> refreshTabStyles());
        refreshTabStyles();

        // ── Status bar ────────────────────────────────────────────────
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        statusBar.setBackground(UIConstants.PRIMARY);
        JLabel status = new JLabel("Ready  |  Bishoftu Conference & Event Management System");
        status.setFont(UIConstants.FONT_SMALL);
        status.setForeground(new Color(200, 210, 255));
        statusBar.add(status);

        root.add(header,    BorderLayout.NORTH);
        root.add(tabs,      BorderLayout.CENTER);
        root.add(statusBar, BorderLayout.SOUTH);
    }

    // ── Tab helpers ───────────────────────────────────────────────────────

    private void addStyledTab(String text, Component component, String iconFile) {
        tabs.addTab("", component);
        int idx = tabs.getTabCount() - 1;
        JLabel lbl = new JLabel(text);
        lbl.setBorder(new EmptyBorder(12, 18, 12, 18));
        // White icon so it shows against the dark-blue sidebar
        if (iconFile != null) {
            javax.swing.ImageIcon ic = UIConstants.loadIconWhite(iconFile);
            if (ic != null) lbl.setIcon(ic);
        }
        lbl.setIconTextGap(10);
        tabLabels.add(lbl);
        tabs.setTabComponentAt(idx, lbl);
    }

    private void refreshTabStyles() {
        int sel = tabs.getSelectedIndex();
        for (int i = 0; i < tabLabels.size(); i++) {
            JLabel l = tabLabels.get(i);
            boolean active = (i == sel);
            // Active tab: gold background + dark-blue bold text for strong contrast
            // Inactive tab: dark-blue background + white regular text
            l.setFont(active ? UIConstants.FONT_BOLD : UIConstants.FONT_BODY);
            l.setForeground(active ? UIConstants.PRIMARY : Color.WHITE);
            l.setBackground(active ? UIConstants.SECONDARY : UIConstants.PRIMARY);
            l.setOpaque(true);   // always opaque — sidebar is always dark
            l.repaint();
        }
    }

    private void buildMenu() {
        JMenuBar bar = new JMenuBar();
        bar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        // ── File menu ─────────────────────────────────────────────────
        JMenu mFile = darkMenu("File");
        JMenuItem miLogout = darkItem("Logout");
        JMenuItem miExit   = darkItem("Exit");
        mFile.add(miLogout);
        mFile.addSeparator();
        mFile.add(miExit);

        // ── Manage menu ───────────────────────────────────────────────
        JMenu mManage = darkMenu("Manage");
        JMenuItem miHalls     = darkItem("Halls");
        JMenuItem miEvents    = darkItem("Events");
        JMenuItem miEmployees = darkItem("Employees");
        mManage.add(miHalls);
        mManage.add(miEvents);
        mManage.add(miEmployees);

        // ── Account menu ──────────────────────────────────────────────
        JMenu mAccount = darkMenu("Account");
        JMenuItem miProfile  = darkItem("My Profile");
        JMenuItem miPassword = darkItem("Change Password");
        mAccount.add(miProfile);
        mAccount.add(miPassword);

        // ── Help menu ─────────────────────────────────────────────────
        JMenu mHelp = darkMenu("Help");
        JMenuItem miAbout = darkItem("About");
        mHelp.add(miAbout);

        bar.add(mFile);
        bar.add(mManage);
        bar.add(mAccount);
        bar.add(mHelp);
        setJMenuBar(bar);

        // ── Menu actions ─────────────────────────────────────────────
        miHalls.addActionListener(e     -> tabs.setSelectedIndex(0));
        miEvents.addActionListener(e    -> tabs.setSelectedIndex(1));
        miEmployees.addActionListener(e -> tabs.setSelectedIndex(2));

        miLogout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this, "Logout and return to login screen?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });

        miExit.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this, "Exit the application?",
                "Confirm Exit", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) System.exit(0);
        });

        miProfile.addActionListener(e  -> showProfileDialog());
        miPassword.addActionListener(e -> showChangePasswordDialog());

        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            "<html><b>Bishoftu Conference Center</b><br>" +
            "Event &amp; Conference Management System<br>" +
            "Version 1.0<br><br>" +
            "Manages halls, events and assigned employees.</html>",
            "About", JOptionPane.INFORMATION_MESSAGE));
    }

    // ── Profile dialog ────────────────────────────────────────────────────

    private void showProfileDialog() {
        JDialog dlg = new JDialog(this, "My Profile", true);
        dlg.setSize(380, 360);
        dlg.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(5,4,5,4);

        String[][] rows = {
            {"First Name",   loggedIn.getFirstName()},
            {"Last Name",    loggedIn.getLastName()},
            {"Phone",        loggedIn.getPhone()},
            {"Username",     loggedIn.getUsername()},
            {"Experience",   loggedIn.getYearsOfExperience() + " years"},
            {"Date of Birth", loggedIn.getDateOfBirth()},
            {"Gender",       loggedIn.getGender() == null ? "—" : loggedIn.getGender()},
            {"Role",         loggedIn.getRole()}
        };
        for (int i = 0; i < rows.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0.35;
            JLabel lbl = new JLabel(rows[i][0] + ":");
            lbl.setFont(UIConstants.FONT_BOLD);
            lbl.setForeground(UIConstants.PRIMARY);
            p.add(lbl, g);
            g.gridx = 1; g.weightx = 0.65;
            JLabel val = new JLabel(rows[i][1]);
            val.setFont(UIConstants.FONT_BODY);
            p.add(val, g);
        }

        dlg.setContentPane(p);
        dlg.setVisible(true);
    }

    // ── Change-password dialog ────────────────────────────────────────────

    private void showChangePasswordDialog() {
        JPasswordField oldPw  = new JPasswordField(16);
        JPasswordField newPw  = new JPasswordField(16);
        JPasswordField conf   = new JPasswordField(16);

        Object[] fields = { "Current password:", oldPw, "New password:", newPw, "Confirm new:", conf };
        int res = JOptionPane.showConfirmDialog(
            this, fields, "Change Password", JOptionPane.OK_CANCEL_OPTION);

        if (res != JOptionPane.OK_OPTION) return;

        String old  = new String(oldPw.getPassword());
        String nw   = new String(newPw.getPassword());
        String cfm  = new String(conf.getPassword());

        if (!old.equals(loggedIn.getPassword())) {
            JOptionPane.showMessageDialog(this, "Current password is incorrect.", "Error",
                JOptionPane.ERROR_MESSAGE); return;
        }
        if (nw.length() < 6) {
            JOptionPane.showMessageDialog(this, "New password must be at least 6 characters.", "Error",
                JOptionPane.ERROR_MESSAGE); return;
        }
        if (!nw.equals(cfm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error",
                JOptionPane.ERROR_MESSAGE); return;
        }
        try {
            new AdminDAO().updatePassword(loggedIn.getEmployeeId(), nw);
            loggedIn.setPassword(nw);
            JOptionPane.showMessageDialog(this, "Password updated successfully.", "Success",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Menu styling helpers ──────────────────────────────────────────────

    private JMenu darkMenu(String text) {
        JMenu m = new JMenu(text);
        m.setFont(UIConstants.FONT_BOLD);
        m.setForeground(Color.BLACK);
        return m;
    }

    private JMenuItem darkItem(String text) {
        JMenuItem mi = new JMenuItem(text);
        mi.setFont(UIConstants.FONT_BODY);
        mi.setForeground(Color.BLACK);
        return mi;
    }
}
