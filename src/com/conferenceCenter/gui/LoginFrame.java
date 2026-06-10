package com.conferenceCenter.gui;

import com.conferenceCenter.dao.AdminDAO;
import com.conferenceCenter.model.Admin;
import com.conferenceCenter.util.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField     tfUsername = new JTextField(20);
    private final JPasswordField tfPassword = new JPasswordField(20);
    private final JLabel         lblStatus  = new JLabel(" ");

    public LoginFrame() {
        setTitle("Bishoftu Conference Center — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(480, 420);
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        // ── Background panel ──────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(
                    0, 0, UIConstants.PRIMARY,
                    0, getHeight(), new Color(63, 81, 181)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        setContentPane(root);

        // ── Header ────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(28, 20, 10, 20));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel icon  = new JLabel("🏛", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icon.setForeground(UIConstants.SECONDARY);
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Bishoftu Conference Center", SwingConstants.CENTER);
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Event & Conference Management System", SwingConstants.CENTER);
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(new Color(200, 210, 255));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(icon);
        header.add(Box.createVerticalStrut(6));
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);

        // ── Login card ────────────────────────────────────────────────
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1),
            new EmptyBorder(24, 32, 24, 32)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        JLabel lUser = fieldLabel("Username");
        JLabel lPass = fieldLabel("Password");
        styleField(tfUsername);
        styleField(tfPassword);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        JLabel cardTitle = new JLabel("Admin Login");
        cardTitle.setFont(UIConstants.FONT_H2);
        cardTitle.setForeground(UIConstants.PRIMARY);
        card.add(cardTitle, gbc);

        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        card.add(lUser, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        card.add(tfUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        card.add(lPass, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        card.add(tfPassword, gbc);

        JButton btnLogin = styledButton("  Login  ");
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.insets = new Insets(14, 4, 4, 4);
        card.add(btnLogin, gbc);

        lblStatus.setFont(UIConstants.FONT_SMALL);
        lblStatus.setForeground(UIConstants.DANGER);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4; gbc.insets = new Insets(2, 4, 2, 4);
        card.add(lblStatus, gbc);

        // ── Wrap card ────────────────────────────────────────────────
        JPanel cardWrapper = new JPanel(new GridBagLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.setBorder(new EmptyBorder(8, 40, 28, 40));
        cardWrapper.add(card);

        root.add(header,     BorderLayout.NORTH);
        root.add(cardWrapper, BorderLayout.CENTER);

        // ── Footer ───────────────────────────────────────────────────
        JLabel footer = new JLabel("Bishoftu Conference & Event Management System", SwingConstants.CENTER);
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(new Color(180, 190, 230));
        footer.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(footer, BorderLayout.SOUTH);

        // ── Actions ──────────────────────────────────────────────────
        btnLogin.addActionListener(e -> doLogin());
        // setDefaultButton makes Enter trigger login anywhere in the window,
        // so no separate KeyListener on the password field is needed.
        getRootPane().setDefaultButton(btnLogin);
    }

    private void doLogin() {
        String user = tfUsername.getText().trim();
        String pass = new String(tfPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            lblStatus.setText("Please enter username and password.");
            return;
        }
        try {
            Admin admin = new AdminDAO().authenticate(user, pass);
            if (admin != null) {
                dispose();
                new MainFrame(admin).setVisible(true);
            } else {
                lblStatus.setText("Invalid username or password.");
                tfPassword.setText("");
            }
        } catch (Exception ex) {
            lblStatus.setText("DB error: " + ex.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_BOLD);
        l.setForeground(UIConstants.PRIMARY);
        return l;
    }

    private void styleField(JTextField f) {
        f.setFont(UIConstants.FONT_BODY);
        f.setBackground(UIConstants.INPUT_BG);
        // 8px top/bottom padding lets the font breathe; the line-border adds 1px each side.
        // We do NOT call setPreferredSize — the layout manager derives height from font + insets,
        // which prevents the vertical clipping that a hard-coded pixel height causes.
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        // Constrain only the width so the column stays a reasonable size.
        f.setMinimumSize(new Dimension(180, 35));
        f.setPreferredSize(new Dimension(220, 36));
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_BUTTON);
        b.setBackground(UIConstants.SECONDARY);
        b.setForeground(UIConstants.PRIMARY);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(200, 38));
        return b;
    }
}
