package eventmanager;

import javax.swing.*;
import java.awt.*;

/**
 * Simple login dialog used to authenticate event coordinators.
 */
public class LoginDialog extends JDialog {

    private static final String DEFAULT_USERNAME = "Group1";
    private static final String DEFAULT_PASSWORD = "admin123";

    private boolean authenticated;

    public LoginDialog(Frame owner) {
        super(owner, "Coordinator Login", true);
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(owner);
        getRootPane().setDefaultButton(loginButton);
    }

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JButton loginButton = new JButton("Login");

    private JPanel buildContent() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Coordinator Access");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.PRIMARY_DARK);
        panel.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.setOpaque(false);
        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        Theme.styleCard(card);
        card.setBackground(Theme.CARD_BG);
        card.add(form);

        panel.add(card, BorderLayout.CENTER);

        loginButton.addActionListener(e -> authenticate());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        Theme.styleButton(loginButton);
        Theme.styleSecondaryButton(cancelButton);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(cancelButton);
        buttons.add(loginButton);
        panel.add(buttons, BorderLayout.SOUTH);

        return panel;
    }

    private void authenticate() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password)) {
            authenticated = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid credentials. Try coordinator/admin123.",
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}

