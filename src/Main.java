import com.conferenceCenter.dao.DatabaseConnection;
import com.conferenceCenter.gui.LoginFrame;

import javax.swing.*;

/**
 * Application entry point.
 * Boots the database then launches the Swing Login screen.
 */
public class Main {
    public static void main(String[] args) {
        // Use system look-and-feel for native decorations
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Bootstrap DB on the calling thread before anything touches Swing
        try {
            DatabaseConnection.initializeSchema();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Database initialisation failed:\n" + e.getMessage() +
                "\n\nMake sure sqlite-jdbc-*.jar is in the 'lib' folder.",
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // All Swing work must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
