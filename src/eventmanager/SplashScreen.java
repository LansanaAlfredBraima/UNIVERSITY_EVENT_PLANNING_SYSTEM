package eventmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Simple splash screen shown at application startup.
 */
public class SplashScreen extends JWindow {

    public SplashScreen() {
        initUI();
    }

    private void initUI() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Theme.PRIMARY);
        content.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("University Event Manager", SwingConstants.LEFT);
        title.setForeground(Color.WHITE);
        title.setFont(Theme.TITLE_FONT.deriveFont(20f));

        JLabel subtitle = new JLabel("Loading...", SwingConstants.LEFT);
        subtitle.setForeground(new Color(230, 230, 230));
        subtitle.setFont(Theme.BASE_FONT.deriveFont(12f));

        JPanel text = new JPanel(new GridLayout(0, 1));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);

        content.add(text, BorderLayout.CENTER);

        // small footer progress bar
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorder(new EmptyBorder(8, 0, 0, 0));
        content.add(bar, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(480, 220);
        // center on screen
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - getWidth()) / 2, (screen.height - getHeight()) / 2);
    }

}
