package eventmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Centralized palette and widget helpers for an elevated UI.
 */
public final class Theme {
    // Softer, modern palette for better contrast and accessibility
    public static Color PRIMARY = new Color(21, 101, 192);
    public static Color PRIMARY_DARK = new Color(13, 71, 161);
    public static Color ACCENT = new Color(0, 150, 136);
    public static Color BACKGROUND = new Color(247, 249, 253);
    public static Color CARD_BG = new Color(255, 255, 255);
    public static Color BORDER = new Color(229, 234, 246);
    public static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font TITLE_FONT = BASE_FONT.deriveFont(Font.BOLD, 22f);

    private Theme() {
    }

    public static void styleDisabled(JComponent component) {
        component.setOpaque(true);
        component.setBackground(new Color(250, 251, 255));
        component.setForeground(new Color(100, 100, 110));
    }

    public static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
            // Keep default L&F if Nimbus is unavailable.
        }
        UIManager.put("control", BACKGROUND);
        UIManager.put("Table.font", BASE_FONT);
        UIManager.put("Label.font", BASE_FONT);
        UIManager.put("Button.font", BASE_FONT.deriveFont(Font.BOLD));
    }

    public static void applyTheme(boolean dark) {
        if (dark) {
            PRIMARY = new Color(33, 150, 243);
            PRIMARY_DARK = new Color(21, 101, 192);
            ACCENT = new Color(255, 193, 7);
            BACKGROUND = new Color(34, 34, 34);
            CARD_BG = new Color(48, 48, 48);
            BORDER = new Color(70, 70, 70);
        } else {
            PRIMARY = new Color(21, 101, 192);
            PRIMARY_DARK = new Color(13, 71, 161);
            ACCENT = new Color(0, 150, 136);
            BACKGROUND = new Color(247, 249, 253);
            CARD_BG = new Color(255, 255, 255);
            BORDER = new Color(229, 234, 246);
        }
        UIManager.put("control", BACKGROUND);
    }

    public static void saveSettings(java.util.Properties props) {
        props.setProperty("theme.dark", String.valueOf(!BACKGROUND.equals(new Color(247, 249, 253))));
    }

    public static void loadSettings(java.util.Properties props) {
        String dark = props.getProperty("theme.dark", "false");
        applyTheme(Boolean.parseBoolean(dark));
    }

    public static void styleCard(JComponent component) {
        component.setOpaque(true);
        component.setBackground(CARD_BG);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 12, 12, 12)
        ));
    }

    public static void styleButton(AbstractButton button) {
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(BASE_FONT.deriveFont(Font.BOLD));
    }

    public static void styleSecondaryButton(AbstractButton button) {
        button.setBackground(PRIMARY_DARK);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(BASE_FONT.deriveFont(Font.BOLD));
    }
}

