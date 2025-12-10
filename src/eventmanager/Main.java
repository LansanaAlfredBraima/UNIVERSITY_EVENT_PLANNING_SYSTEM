package eventmanager;

import javax.swing.*;

/**
 * Application entry point.
 */
public class Main {
    public static void main(String[] args) {
        Theme.applyLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            // Show splash for 5 seconds, then proceed to login
            SplashScreen splash = new SplashScreen();
            splash.setVisible(true);

            // After 5 seconds dispose splash and continue with login flow
            javax.swing.Timer t = new javax.swing.Timer(5000, ae -> {
                splash.dispose();
                DatabaseHelper databaseHelper = new DatabaseHelper();
                LoginDialog loginDialog = new LoginDialog(null);
                loginDialog.setVisible(true);
                if (loginDialog.isAuthenticated()) {
                    EventManagerFrame frame = new EventManagerFrame(databaseHelper);
                    frame.setVisible(true);
                } else {
                    System.exit(0);
                }
            });
            t.setRepeats(false);
            t.start();
        });
    }
}

