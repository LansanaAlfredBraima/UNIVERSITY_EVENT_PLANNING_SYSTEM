package eventmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Main Swing window used to manage university events and participants.
 */
public class EventManagerFrame extends JFrame {

    private final DatabaseHelper databaseHelper;
    private final List<UniversityEvent> events;

    private final DefaultTableModel eventTableModel;
    private final JTable eventTable;
    private final JTable participantTable;
    private final DefaultTableModel participantTableModel;

    private final JTextField eventIdField = new JTextField();
    // Replaced free-text name field with a dropdown containing the requested events
    private final JComboBox<String> nameBox = new JComboBox<>(new String[] {
            "AI & Machine Learning Seminar",
            "University Football League Finals",
            "Modern Web Development Workshop",
            "Inter-Faculty Cultural Night",
            "Athletics",
            "Campus Art & Creative Expo",
            "New Student Orientation Week",
            "Annual Job & Career Fair"
    });
    private final JComboBox<String> venueBox = new JComboBox<>(new String[] {
            "Gallery", "Library", "Innovation Hub", "Bintumani Conference Center"
    });
    private final JComboBox<String> organizerBox = new JComboBox<>(new String[] {
            "Sam", "Ruben", "Mtheus", "Bruno"
    });
    // Updated category dropdown to the requested categories
    private final JComboBox<String> categoryBox = new JComboBox<>(new String[] {
            "Seminar",
            "Sports",
            "Workshop",
            "Cultural Show",
            "Exhibition",
            "Orientation",
            "Career Fair"
    });
    // Mapping from event name → category for auto-selection
    private final java.util.Map<String, String> nameCategoryMap = new java.util.HashMap<>();
    private final JSpinner dateSpinner = new JSpinner(
            new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_YEAR));
    // time spinner for event time (hours:minutes)
    private final JSpinner timeSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE));
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel totalEventsLabel = new JLabel("0");
    private final JLabel totalParticipantsLabel = new JLabel("0");
    private int eventCounter;
    private int participantCounter;
    // Notification scheduler and state
    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
            .newSingleThreadScheduledExecutor();
    private final java.util.Set<String> notifiedEvents = new java.util.HashSet<>();
    private boolean notificationsEnabled = true;
    private static final String SETTINGS_FILE = "data/settings.properties";

    public EventManagerFrame(DatabaseHelper databaseHelper) {
        super("University Event Manager");
        this.databaseHelper = databaseHelper;
        this.events = new ArrayList<>(databaseHelper.loadEvents());

        // Load persisted settings (theme)
        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
        } catch (Exception ignored) {
        }
        Theme.loadSettings(props);

        this.eventTableModel = new DefaultTableModel(
                new Object[] { "Event ID", "Name", "Date & Time", "Venue", "Organizer", "Category", "Participants" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.participantTableModel = new DefaultTableModel(
                new Object[] { "Participant ID", "Full Name", "Type" },
                0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.eventTable = new JTable(eventTableModel);
        this.participantTable = new JTable(participantTableModel);
        this.eventTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.eventTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelection();
            }
        });

        // Event ID is auto-generated and should not be editable
        eventIdField.setEditable(false);
        Theme.styleDisabled(eventIdField);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));
        // Make the date editor visually editable
        try {
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) dateSpinner.getEditor();
            editor.getTextField().setBackground(Color.WHITE);
            editor.getTextField().setFont(Theme.BASE_FONT);
        } catch (Exception ignored) {
        }

        initializeCounters();
        // initialize the mapping and wire auto-selection
        initNameCategoryMap();
        nameBox.addActionListener(e -> autoSelectCategory());

        // Make category non-editable; keep date editable as requested
        categoryBox.setEnabled(false);
        // Style disabled control for consistent look
        Theme.styleDisabled(categoryBox);
        dateSpinner.setEnabled(true);

        configureTables();

        // Start notification scheduler (checks every minute)
        startNotificationScheduler();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1150, 700));
        setLocationRelativeTo(null);

        setContentPane(buildContent());
        refreshEventTable();
        eventIdField.setText(formatEventId(eventCounter));
    }

    private Container buildContent() {
        JPanel root = new JPanel(new BorderLayout(16, 16));
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.setBackground(Theme.BACKGROUND);

        root.add(buildHeroPanel(), BorderLayout.NORTH);
        root.add(buildEventTableSection(), BorderLayout.CENTER);
        JScrollPane editorScroll = new JScrollPane(buildEditorPanel(), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        editorScroll.setPreferredSize(new Dimension(360, 0));
        editorScroll.setBorder(null);
        root.add(editorScroll, BorderLayout.EAST);
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        return root;
    }

    private JComponent buildHeroPanel() {
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, Theme.PRIMARY,
                        getWidth(), getHeight(), Theme.PRIMARY_DARK);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        // Reduce hero height and padding so the editor area has more vertical space
        hero.setPreferredSize(new Dimension(0, 88));
        hero.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("University Events Dashboard");
        title.setFont(Theme.TITLE_FONT.deriveFont(18f));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Plan seminars, workshops, and cultural shows in one place.");
        subtitle.setForeground(new Color(235, 238, 246));
        subtitle.setFont(Theme.BASE_FONT.deriveFont(12f));

        JPanel textPanel = new JPanel(new GridLayout(0, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        hero.add(textPanel, BorderLayout.WEST);

        JPanel metrics = new JPanel(new GridLayout(1, 0, 16, 0));
        metrics.setOpaque(false);
        metrics.add(buildMetricCard("Events", totalEventsLabel));
        metrics.add(buildMetricCard("Participants", totalParticipantsLabel));
        hero.add(metrics, BorderLayout.EAST);

        return hero;
    }

    private JComponent buildEventTableSection() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);
        Theme.styleCard(panel);
        JLabel header = new JLabel("Scheduled Events");
        header.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 18f));
        panel.add(header, BorderLayout.NORTH);
        panel.add(new JScrollPane(eventTable), BorderLayout.CENTER);
        panel.add(buildParticipantPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildParticipantPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setOpaque(false);
        JLabel label = new JLabel("Participants");
        label.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 16f));
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(participantTable), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(100, 200));
        return panel;
    }

    private JComponent buildEditorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        // Provide a reasonable preferred height so the editor scroll pane can enable
        // vertical scrolling
        panel.setPreferredSize(new Dimension(360, 600));
        panel.setOpaque(false);

        JPanel form = new JPanel(new GridBagLayout());
        Theme.styleCard(form);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        addFormRow(form, gbc, "Event ID", eventIdField);
        addFormRow(form, gbc, "Name", nameBox); // use the combo box here
        addFormRow(form, gbc, "Date", dateSpinner);
        addFormRow(form, gbc, "Time", timeSpinner);
        addFormRow(form, gbc, "Venue", venueBox);
        addFormRow(form, gbc, "Organizer", organizerBox);
        addFormRow(form, gbc, "Category", categoryBox);

        panel.add(form);
        panel.add(Box.createVerticalStrut(12));
        panel.add(buildButtonPanel());

        return panel;
    }

    private void addFormRow(JPanel form, GridBagConstraints gbc, String label, JComponent component) {
        gbc.gridx = 0;
        gbc.weightx = 0;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        component.setPreferredSize(new Dimension(200, 28));
        form.add(component, gbc);
        gbc.gridy++;
    }

    private JComponent buildButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.setOpaque(false);
        JButton addButton = new JButton("Add Event");
        addButton.addActionListener(e -> addEvent());
        JButton updateButton = new JButton("Update Event");
        updateButton.addActionListener(e -> updateEvent());
        JButton deleteButton = new JButton("Delete Event");
        deleteButton.addActionListener(e -> deleteEvent());
        JButton registerButton = new JButton("Register Participant");
        registerButton.addActionListener(e -> registerParticipant());
        JButton reportButton = new JButton("Generate Reports");
        reportButton.addActionListener(e -> showReports());

        Theme.styleButton(addButton);
        Theme.styleButton(updateButton);
        deleteButton.setBackground(new Color(220, 68, 55));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        deleteButton.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        registerButton.setBackground(Theme.ACCENT);
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        registerButton.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        Theme.styleSecondaryButton(reportButton);

        panel.add(addButton);
        panel.add(updateButton);
        panel.add(deleteButton);
        panel.add(registerButton);
        panel.add(reportButton);
        return panel;
    }

    private JComponent buildStatusBar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        panel.setBackground(Theme.CARD_BG);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER));
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setFont(Theme.BASE_FONT.deriveFont(Font.ITALIC));
        panel.add(statusLabel, BorderLayout.CENTER);

        // Theme toggle on the right
        JToggleButton themeToggle = new JToggleButton("Dark");
        themeToggle.setSelected(!Theme.BACKGROUND.equals(new Color(247, 249, 253)));
        themeToggle.addActionListener(e -> {
            boolean dark = themeToggle.isSelected();
            Theme.applyTheme(dark);
            // persist
            java.util.Properties props = new java.util.Properties();
            Theme.saveSettings(props);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(SETTINGS_FILE)) {
                props.store(fos, "app settings");
            } catch (Exception ignored) {
            }
            // Rebuild UI with new theme
            SwingUtilities.invokeLater(() -> {
                setContentPane(buildContent());
                revalidate();
                repaint();
                refreshEventTable();
            });
        });
        panel.add(themeToggle, BorderLayout.EAST);
        return panel;
    }

    private void populateFormFromSelection() {
        int row = eventTable.getSelectedRow();
        if (row < 0 || row >= events.size()) {
            clearForm();
            participantTableModel.setRowCount(0);
            return;
        }
        UniversityEvent selected = events.get(row);
        eventIdField.setText(selected.getEventId());
        nameBox.setSelectedItem(selected.getName()); // set selection on the combo box
        dateSpinner.setValue(Date.from(selected.getDate()
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        if (selected.getTime() != null) {
            timeSpinner.setValue(Date.from(selected.getTime()
                    .atDate(LocalDate.now())
                    .atZone(ZoneId.systemDefault()).toInstant()));
        }
        venueBox.setSelectedItem(selected.getVenue());
        organizerBox.setSelectedItem(selected.getOrganizer());
        categoryBox.setSelectedItem(selected.getCategory());
        loadParticipants(selected);
    }

    private void loadParticipants(UniversityEvent event) {
        participantTableModel.setRowCount(0);
        for (Participant participant : event.getParticipants()) {
            participantTableModel.addRow(new Object[] {
                    participant.getParticipantId(),
                    participant.getFullName(),
                    participant.getType()
            });
        }
    }

    private void clearForm() {
        eventIdField.setText(formatEventId(eventCounter));
        nameBox.setSelectedIndex(0); // reset to first dropdown value
        autoSelectCategory();
        dateSpinner.setValue(new Date());
        venueBox.setSelectedIndex(0);
        organizerBox.setSelectedIndex(0);
        // categoryBox now set by autoSelectCategory when applicable
    }

    private Optional<UniversityEvent> findEventById(String eventId) {
        return events.stream()
                .filter(ev -> ev.getEventId().equalsIgnoreCase(eventId))
                .findFirst();
    }

    private Optional<UniversityEvent> findEventByName(String name) {
        return events.stream()
                .filter(ev -> ev.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    private void addEvent() {
        eventTable.clearSelection();
        eventIdField.setText(formatEventId(eventCounter));
        try {
            UniversityEvent event = buildEventFromForm(false);
            // Duplicate ID handling: offer to auto-generate a new ID
            Optional<UniversityEvent> existingById = findEventById(event.getEventId());
            if (existingById.isPresent()) {
                int choice = JOptionPane.showOptionDialog(this,
                        "An event with this ID already exists. What would you like to do?",
                        "Duplicate ID",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new String[] { "Auto-generate new ID", "Cancel" },
                        "Auto-generate new ID");
                if (choice != 0) {
                    return;
                }
                // auto-generate
                event.setEventId(formatEventId(eventCounter));
            }

            // Duplicate name handling: offer to auto-rename or continue
            Optional<UniversityEvent> existingByName = findEventByName(event.getName());
            if (existingByName.isPresent()) {
                int choice = JOptionPane.showOptionDialog(this,
                        "An event with this name already exists. Rename automatically or cancel?",
                        "Duplicate Name",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new String[] { "Auto-rename", "Cancel" },
                        "Auto-rename");
                if (choice != 0) {
                    return;
                }
                // auto-rename by appending a counter
                String base = event.getName();
                int suffix = 2;
                while (findEventByName(base + " (" + suffix + ")").isPresent()) {
                    suffix++;
                }
                event.setName(base + " (" + suffix + ")");
            }
            events.add(event);
            eventCounter = extractTrailingNumber(event.getEventId()) + 1;
            persistAndRefresh("Event added successfully.");
            selectEvent(event);
            // Automatically open participant registration after creating an event
            registerParticipant();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void updateEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select an event to update.");
            return;
        }
        try {
            UniversityEvent updatedData = buildEventFromForm(true);
            UniversityEvent original = events.get(selectedRow);
            if (!original.getEventId().equalsIgnoreCase(updatedData.getEventId())
                    && findEventById(updatedData.getEventId()).isPresent()) {
                showError("Another event already uses this ID.");
                return;
            }
            // Build a summary of changes for user confirmation
            StringBuilder changes = new StringBuilder();
            if (!original.getEventId().equals(updatedData.getEventId())) {
                changes.append("Event ID: ").append(original.getEventId())
                        .append(" -> ").append(updatedData.getEventId()).append("\n");
            }
            if (!original.getName().equals(updatedData.getName())) {
                changes.append("Name: ").append(original.getName())
                        .append(" -> ").append(updatedData.getName()).append("\n");
            }
            if (!original.getDate().equals(updatedData.getDate())) {
                changes.append("Date: ").append(original.getDate())
                        .append(" -> ").append(updatedData.getDate()).append("\n");
            }
            if (!original.getVenue().equals(updatedData.getVenue())) {
                changes.append("Venue: ").append(original.getVenue())
                        .append(" -> ").append(updatedData.getVenue()).append("\n");
            }
            if (!original.getOrganizer().equals(updatedData.getOrganizer())) {
                changes.append("Organizer: ").append(original.getOrganizer())
                        .append(" -> ").append(updatedData.getOrganizer()).append("\n");
            }
            if (!original.getCategory().equals(updatedData.getCategory())) {
                changes.append("Category: ").append(original.getCategory())
                        .append(" -> ").append(updatedData.getCategory()).append("\n");
            }

            if (changes.length() == 0) {
                statusLabel.setText("No changes to update.");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "You are about to apply the following changes:\n\n" + changes.toString() + "\nProceed?",
                    "Confirm Update",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                statusLabel.setText("Update cancelled.");
                return;
            }

            // If updating ID/name, check for conflicts and offer auto-resolve
            if (!original.getEventId().equalsIgnoreCase(updatedData.getEventId())) {
                Optional<UniversityEvent> conflict = findEventById(updatedData.getEventId());
                if (conflict.isPresent() && conflict.get() != original) {
                    int choice = JOptionPane.showOptionDialog(this,
                            "Another event already uses this ID. Auto-generate a new ID or cancel?",
                            "ID Conflict",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            new String[] { "Auto-generate", "Cancel" },
                            "Auto-generate");
                    if (choice != 0) {
                        statusLabel.setText("Update cancelled.");
                        return;
                    }
                    updatedData.setEventId(formatEventId(eventCounter));
                }
            }

            if (!original.getName().equalsIgnoreCase(updatedData.getName())) {
                Optional<UniversityEvent> conflict = findEventByName(updatedData.getName());
                if (conflict.isPresent() && conflict.get() != original) {
                    int choice = JOptionPane.showOptionDialog(this,
                            "Another event already uses this name. Auto-rename or cancel?",
                            "Name Conflict",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            new String[] { "Auto-rename", "Cancel" },
                            "Auto-rename");
                    if (choice != 0) {
                        statusLabel.setText("Update cancelled.");
                        return;
                    }
                    String base = updatedData.getName();
                    int suffix = 2;
                    while (findEventByName(base + " (" + suffix + ")").isPresent()) {
                        suffix++;
                    }
                    updatedData.setName(base + " (" + suffix + ")");
                }
            }

            // Apply confirmed changes
            original.setEventId(updatedData.getEventId());
            original.setName(updatedData.getName());
            original.setDate(updatedData.getDate());
            original.setTime(updatedData.getTime());
            original.setVenue(updatedData.getVenue());
            original.setOrganizer(updatedData.getOrganizer());
            original.setCategory(updatedData.getCategory());

            persistAndRefresh("Event updated successfully.");
            selectEvent(original);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteEvent() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select an event to delete.");
            return;
        }
        UniversityEvent event = events.get(selectedRow);
        int result = JOptionPane.showConfirmDialog(
                this,
                "Delete event \"" + event.getName() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            events.remove(selectedRow);
            if (events.isEmpty()) {
                eventCounter = 1;
            }
            persistAndRefresh("Event deleted.");
            clearForm();
        }
    }

    private void registerParticipant() {
        int selectedRow = eventTable.getSelectedRow();
        if (selectedRow < 0) {
            showError("Select an event first.");
            return;
        }
        UniversityEvent event = events.get(selectedRow);
        // Build a persistent dialog so users can add multiple participants sequentially
        JTextField nameField = new JTextField(20);
        JComboBox<Participant.ParticipantType> typeBox = new JComboBox<>(Participant.ParticipantType.values());
        // Use a per-event counter so new events start from default (PAR-00001)
        int[] perEventCounter = new int[1];
        perEventCounter[0] = event.getParticipants().stream()
                .mapToInt(p -> extractTrailingNumber(p.getParticipantId()))
                .max()
                .orElse(0) + 1;
        JLabel idPreview = new JLabel(formatParticipantId(perEventCounter[0]));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Participant ID"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        form.add(idPreview, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Full Name"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        form.add(nameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Type"), gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;
        form.add(typeBox, gbc);

        JButton addBtn = new JButton("Add");
        JButton doneBtn = new JButton("Done");
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(addBtn);
        btnPanel.add(doneBtn);

        // List to show participants added during this dialog session
        DefaultListModel<String> addedModel = new DefaultListModel<>();
        JList<String> addedList = new JList<>(addedModel);
        addedList.setVisibleRowCount(8);
        JScrollPane addedScroll = new JScrollPane(addedList);
        addedScroll.setPreferredSize(new Dimension(280, 160));

        JDialog dialog = new JDialog(this, "Register Participants", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        Theme.styleCard(root);
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.add(form, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);
        // show added participants on the right
        root.add(addedScroll, BorderLayout.EAST);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        // Add action to add participant but keep dialog open for more
        addBtn.addActionListener(ae -> {
            String fullName = nameField.getText().trim();
            Participant.ParticipantType type = (Participant.ParticipantType) typeBox.getSelectedItem();

            if (fullName.isEmpty()) {
                showError("Participant name is required.");
                return;
            }

            boolean alreadyRegistered = event.getParticipants().stream()
                    .anyMatch(p -> p.getFullName().equalsIgnoreCase(fullName));
            if (alreadyRegistered) {
                showError("This participant is already registered for this event.");
                return;
            }

            String participantId = formatParticipantId(perEventCounter[0]++);
            event.addParticipant(new Participant(participantId, fullName, type));
            // Keep global participantCounter at least as large as any per-event counter
            participantCounter = Math.max(participantCounter, perEventCounter[0]);
            persistAndRefresh("Participant registered.");
            selectEvent(event);
            idPreview.setText(formatParticipantId(perEventCounter[0]));
            // add to session list and clear for next
            addedModel.addElement(participantId + " — " + fullName + " (" + type + ")");
            nameField.setText("");
            nameField.requestFocusInWindow();
        });

        // Close dialog when done
        doneBtn.addActionListener(ae -> dialog.dispose());

        // Keyboard shortcuts: Enter = Add, Esc = Done
        InputMap im = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = dialog.getRootPane().getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "add");
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "done");
        am.put("add", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                addBtn.doClick();
            }
        });
        am.put("done", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                doneBtn.doClick();
            }
        });

        dialog.setVisible(true);
    }

    private void showReports() {
        JDialog dialog = new JDialog(this, "Event Insights", true);
        dialog.setSize(760, 520);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBackground(Theme.BACKGROUND);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
        root.add(buildReportHero(), BorderLayout.NORTH);
        root.add(buildReportTabs(), BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private JPanel buildReportHero() {
        JPanel hero = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, Theme.PRIMARY,
                        getWidth(), getHeight(), Theme.PRIMARY_DARK);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 110));
        hero.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel title = new JLabel("Insightful Event Reports");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Snapshot of upcoming schedules, registrants, and stats.");
        subtitle.setForeground(new Color(235, 238, 246));

        JPanel textPanel = new JPanel(new GridLayout(0, 1));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        JPanel chips = new JPanel(new GridLayout(1, 0, 12, 0));
        chips.setOpaque(false);
        chips.add(buildMetricChip("Events", String.valueOf(events.size())));
        chips.add(buildMetricChip("Participants",
                String.valueOf(events.stream().mapToInt(UniversityEvent::getParticipantCount).sum())));

        hero.add(textPanel, BorderLayout.WEST);
        hero.add(chips, BorderLayout.EAST);
        return hero;
    }

    private JComponent buildMetricChip(String label, String value) {
        JPanel chip = new JPanel(new BorderLayout());
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(6, 12, 6, 12));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(Theme.TITLE_FONT.deriveFont(24f));
        JLabel labelComp = new JLabel(label.toUpperCase());
        labelComp.setForeground(new Color(220, 230, 255));
        labelComp.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 12f));
        chip.add(labelComp, BorderLayout.NORTH);
        chip.add(valueLabel, BorderLayout.CENTER);
        return chip;
    }

    private JTabbedPane buildReportTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 14f));
        tabs.setBackground(Theme.CARD_BG);
        tabs.setOpaque(true);
        tabs.addTab("Upcoming Schedule", buildUpcomingReportTable());
        tabs.addTab("Participant Roster", buildParticipantReportTable());
        tabs.addTab("Statistics", buildStatisticsPanel());

        // Improve tab contrast: default unselected styling then highlight selected tab
        for (int i = 0; i < tabs.getTabCount(); i++) {
            tabs.setBackgroundAt(i, Theme.CARD_BG);
            tabs.setForegroundAt(i, Color.DARK_GRAY);
        }
        if (tabs.getTabCount() > 0) {
            tabs.setBackgroundAt(tabs.getSelectedIndex(), Theme.PRIMARY);
            tabs.setForegroundAt(tabs.getSelectedIndex(), Color.WHITE);
        }

        tabs.addChangeListener(e -> {
            int sel = tabs.getSelectedIndex();
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (i == sel) {
                    tabs.setBackgroundAt(i, Theme.PRIMARY);
                    tabs.setForegroundAt(i, Color.WHITE);
                } else {
                    tabs.setBackgroundAt(i, Theme.CARD_BG);
                    tabs.setForegroundAt(i, Color.DARK_GRAY);
                }
            }
        });

        return tabs;
    }

    private JComponent buildUpcomingReportTable() {
        if (events.isEmpty()) {
            return buildEmptyState("No events scheduled yet.");
        }
        String[] columns = { "Event", "Category", "Date", "Venue", "Organizer", "Participants" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        events.stream()
                .sorted(Comparator.comparing(UniversityEvent::getDate))
                .forEach(ev -> model.addRow(new Object[] {
                        ev.getName(),
                        ev.getCategory(),
                        ev.getDate(),
                        ev.getVenue(),
                        ev.getOrganizer(),
                        ev.getParticipantCount()
                }));
        JTable table = new JTable(model);
        styleReportTable(table);
        table.getColumnModel().getColumn(5).setPreferredWidth(110);
        return new JScrollPane(table);
    }

    private JComponent buildParticipantReportTable() {
        List<Object[]> rows = events.stream()
                .flatMap(ev -> ev.getParticipants().stream()
                        .map(p -> new Object[] { ev.getName(), p.getFullName(), p.getType() }))
                .collect(Collectors.toList());

        if (rows.isEmpty()) {
            return buildEmptyState("No participants have registered yet.");
        }

        String[] columns = { "Event", "Participant", "Type" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        rows.forEach(model::addRow);

        JTable table = new JTable(model);
        styleReportTable(table);
        return new JScrollPane(table);
    }

    private JComponent buildStatisticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 0, 12, 12));
        cards.setOpaque(false);

        int totalEvents = events.size();
        int totalParticipants = events.stream().mapToInt(UniversityEvent::getParticipantCount).sum();
        UniversityEvent busiest = events.stream()
                .max(Comparator.comparingInt(UniversityEvent::getParticipantCount))
                .orElse(null);

        cards.add(buildStatCard("Total Events", String.valueOf(totalEvents), "All scheduled activities"));
        cards.add(buildStatCard("Total Participants", String.valueOf(totalParticipants),
                "Across every event"));
        String busiestText = busiest == null ? "N/A" : busiest.getName() + " (" + busiest.getParticipantCount() + ")";
        cards.add(buildStatCard("Busiest Event", busiestText, "Most popular session"));

        panel.add(cards, BorderLayout.NORTH);

        JTextArea clashArea = new JTextArea();
        clashArea.setEditable(false);
        clashArea.setLineWrap(true);
        clashArea.setWrapStyleWord(true);
        clashArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        String clashText = events.stream()
                .collect(Collectors.groupingBy(ev -> ev.getDate() + "@" + ev.getVenue()))
                .values()
                .stream()
                .filter(list -> list.size() > 1)
                .map(list -> list.get(0).getDate() + " @ " + list.get(0).getVenue()
                        + " -> " + list.stream().map(UniversityEvent::getName).collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));

        if (clashText.isBlank()) {
            clashText = "No venue clashes detected.";
        }
        clashArea.setText("Date/Venue Conflicts\n---------------------\n" + clashText);

        JPanel clashCard = new JPanel(new BorderLayout());
        Theme.styleCard(clashCard);
        clashCard.add(new JScrollPane(clashArea), BorderLayout.CENTER);
        panel.add(clashCard, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildStatCard(String title, String value, String subtitle) {
        JPanel card = new JPanel(new BorderLayout());
        Theme.styleCard(card);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.TITLE_FONT.deriveFont(20f));
        valueLabel.setForeground(Theme.PRIMARY_DARK);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        titleLabel.setForeground(Color.DARK_GRAY);
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setForeground(Color.GRAY);
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLabel, BorderLayout.SOUTH);
        return card;
    }

    private void styleReportTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(Theme.BASE_FONT);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionBackground(Theme.PRIMARY);
        table.setSelectionForeground(Color.WHITE);

        // Header renderer
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setBackground(Theme.PRIMARY_DARK);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        headerRenderer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        table.getTableHeader().setDefaultRenderer(headerRenderer);
        table.getTableHeader().setPreferredSize(new Dimension(table.getTableHeader().getPreferredSize().width, 40));

        // Cell renderer with striping and padding
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(Theme.PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 252));
                    c.setForeground(Color.DARK_GRAY);
                }
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(new EmptyBorder(6, 8, 6, 8));
                }
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    private JComponent buildEmptyState(String message) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD, 15f));
        label.setForeground(Color.GRAY);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private UniversityEvent buildEventFromForm(boolean allowPastDates) {
        String eventId = eventIdField.getText().trim();
        // read name from combo box
        String name = Objects.requireNonNull(nameBox.getSelectedItem()).toString().trim();
        Date dateValue = (Date) dateSpinner.getValue();
        String venue = Objects.requireNonNull(venueBox.getSelectedItem()).toString();
        String organizer = Objects.requireNonNull(organizerBox.getSelectedItem()).toString();
        String category = Objects.requireNonNull(categoryBox.getSelectedItem()).toString();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("Event name is required.");
        }

        LocalDate date = Instant.ofEpochMilli(dateValue.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // read time from timeSpinner
        Date timeValue = (Date) timeSpinner.getValue();
        LocalTime time = Instant.ofEpochMilli(timeValue.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalTime().withSecond(0).withNano(0);

        if (hasClashingEvent(eventId, date, time, venue)) {
            throw new IllegalArgumentException(
                    "Another event is already scheduled at this venue at the same date/time.");
        }

        // Validate event ID pattern (must be like EVT-0001)
        if (eventId.isEmpty()) {
            throw new IllegalArgumentException("Event ID is required.");
        }
        if (!eventId.matches("EVT-\\d{4}")) {
            throw new IllegalArgumentException("Event ID must follow pattern EVT-0001 (e.g. EVT-0001).");
        }

        // Date/time must not be in the past (today allowed but time must be future)
        if (!allowPastDates && (date.isBefore(LocalDate.now())
                || (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())))) {
            throw new IllegalArgumentException("Event date/time cannot be in the past.");
        }

        return new UniversityEvent(eventId, name, date, time, venue, organizer, category);
    }

    private boolean hasClashingEvent(String eventId, LocalDate date, LocalTime time, String venue) {
        return events.stream()
                .filter(ev -> !ev.getEventId().equalsIgnoreCase(eventId))
                .anyMatch(ev -> ev.getDate().equals(date) && Objects.equals(ev.getTime(), time)
                        && ev.getVenue().equalsIgnoreCase(venue));
    }

    private void persistAndRefresh(String statusMessage) {
        databaseHelper.saveEvents(events);
        refreshEventTable();
        statusLabel.setText(statusMessage);
    }

    private void startNotificationScheduler() {
        final int minutesBefore = 10;
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!notificationsEnabled)
                    return;
                // check upcoming events
                for (UniversityEvent ev : events) {
                    if (ev.getDate() == null || ev.getTime() == null)
                        continue;
                    java.time.LocalDateTime eventDateTime = java.time.LocalDateTime.of(ev.getDate(), ev.getTime());
                    java.time.LocalDateTime threshold = java.time.LocalDateTime.now().plusMinutes(minutesBefore);
                    if (!notifiedEvents.contains(ev.getEventId()) && !eventDateTime.isAfter(threshold)
                            && eventDateTime.isAfter(java.time.LocalDateTime.now())) {
                        // show notification
                        showTrayNotification("Upcoming event: " + ev.getName(),
                                ev.getDate() + " " + ev.getTime() + " @ " + ev.getVenue());
                        notifiedEvents.add(ev.getEventId());
                    }
                }
            } catch (Exception ignored) {
            }
        }, 10, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void showTrayNotification(String caption, String text) {
        try {
            if (SystemTray.isSupported()) {
                SystemTray tray = SystemTray.getSystemTray();
                TrayIcon[] icons = tray.getTrayIcons();
                TrayIcon icon;
                if (icons.length > 0)
                    icon = icons[0];
                else {
                    Image img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    icon = new TrayIcon(img);
                    tray.add(icon);
                }
                icon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
            } else {
                // fallback: update status bar
                SwingUtilities.invokeLater(() -> statusLabel.setText(caption + ": " + text));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(caption + ": " + text));
        }
    }

    private void refreshEventTable() {
        eventTableModel.setRowCount(0);
        events.sort(Comparator.comparing(UniversityEvent::getDate));
        for (UniversityEvent event : events) {
            String dt = event.getDate().toString();
            if (event.getTime() != null) {
                dt = event.getDate().toString() + " " + event.getTime().toString();
            }
            eventTableModel.addRow(new Object[] {
                    event.getEventId(),
                    event.getName(),
                    dt,
                    event.getVenue(),
                    event.getOrganizer(),
                    event.getCategory(),
                    event.getParticipantCount()
            });
        }
        totalEventsLabel.setText(String.valueOf(events.size()));
        totalParticipantsLabel.setText(String.valueOf(events.stream()
                .mapToInt(UniversityEvent::getParticipantCount)
                .sum()));
    }

    private void selectEvent(UniversityEvent event) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i) == event) {
                eventTable.setRowSelectionInterval(i, i);
                eventTable.scrollRectToVisible(eventTable.getCellRect(i, 0, true));
                populateFormFromSelection();
                break;
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message);
    }

    private void configureTables() {
        // General table polish: remove heavy grid lines, add row padding and striping
        eventTable.setFillsViewportHeight(true);
        eventTable.setRowHeight(36);
        eventTable.setShowVerticalLines(false);
        // Show subtle grid lines for clarity
        eventTable.setShowGrid(true);
        eventTable.setShowHorizontalLines(true);
        eventTable.setShowVerticalLines(true);
        eventTable.setIntercellSpacing(new Dimension(1, 1));
        eventTable.setSelectionBackground(Theme.PRIMARY);
        eventTable.setSelectionForeground(Color.WHITE);
        eventTable.setGridColor(Theme.BORDER);
        eventTable.setFont(Theme.BASE_FONT);

        // Header renderer for consistent contrast across look-and-feels
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setBackground(Theme.PRIMARY_DARK);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        headerRenderer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER));
        eventTable.getTableHeader().setDefaultRenderer(headerRenderer);
        eventTable.getTableHeader()
                .setPreferredSize(new Dimension(eventTable.getTableHeader().getPreferredSize().width, 40));

        // Cell renderer with alternating row colors and padding
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(Theme.PRIMARY);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 252));
                    c.setForeground(Color.DARK_GRAY);
                }
                if (c instanceof JComponent) {
                    ((JComponent) c).setBorder(new EmptyBorder(6, 8, 6, 8));
                }
                return c;
            }
        };

        // Apply renderer to all event table columns
        for (int i = 0; i < eventTable.getColumnCount(); i++) {
            eventTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        // Center the participants count column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        eventTable.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        // Participant table styling (match event table)
        participantTable.setFillsViewportHeight(true);
        participantTable.setRowHeight(32);
        participantTable.setShowVerticalLines(false);
        participantTable.setShowGrid(true);
        participantTable.setShowHorizontalLines(true);
        participantTable.setShowVerticalLines(true);
        participantTable.setIntercellSpacing(new Dimension(1, 1));
        participantTable.setSelectionBackground(new Color(244, 221, 130));
        participantTable.setSelectionForeground(Color.DARK_GRAY);
        participantTable.setFont(Theme.BASE_FONT);

        participantTable.getTableHeader().setDefaultRenderer(headerRenderer);
        participantTable.getTableHeader()
                .setPreferredSize(new Dimension(participantTable.getTableHeader().getPreferredSize().width, 36));
        for (int i = 0; i < participantTable.getColumnCount(); i++) {
            participantTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    private JComponent buildMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        Theme.styleCard(card);
        card.setPreferredSize(new Dimension(160, 70));

        valueLabel.setFont(Theme.TITLE_FONT.deriveFont(30f));
        valueLabel.setForeground(Theme.PRIMARY_DARK);

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(Theme.BASE_FONT.deriveFont(Font.BOLD));
        titleLabel.setForeground(Color.GRAY);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void initializeCounters() {
        eventCounter = events.stream()
                .mapToInt(ev -> extractTrailingNumber(ev.getEventId()))
                .max()
                .orElse(0) + 1;
        participantCounter = events.stream()
                .flatMap(ev -> ev.getParticipants().stream())
                .mapToInt(p -> extractTrailingNumber(p.getParticipantId()))
                .max()
                .orElse(0) + 1;
    }

    private int extractTrailingNumber(String value) {
        if (value == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d+)$").matcher(value);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Populate the name→category map with the project's requested mappings.
     */
    private void initNameCategoryMap() {
        nameCategoryMap.put("AI & Machine Learning Seminar", "Seminar");
        nameCategoryMap.put("University Football League Finals", "Sports");
        nameCategoryMap.put("Modern Web Development Workshop", "Workshop");
        nameCategoryMap.put("Inter-Faculty Cultural Night", "Cultural Show");
        nameCategoryMap.put("Athletics", "Sports");
        nameCategoryMap.put("Campus Art & Creative Expo", "Exhibition");
        nameCategoryMap.put("New Student Orientation Week", "Orientation");
        nameCategoryMap.put("Annual Job & Career Fair", "Career Fair");
    }

    /**
     * If the selected name has a mapped category, apply it to the category box.
     */
    private void autoSelectCategory() {
        Object sel = nameBox.getSelectedItem();
        if (sel == null)
            return;
        String mapped = nameCategoryMap.get(sel.toString());
        if (mapped != null) {
            categoryBox.setSelectedItem(mapped);
        }
    }

    private String formatEventId(int number) {
        return String.format("EVT-%04d", Math.max(1, number));
    }

    private String formatParticipantId(int number) {
        return String.format("PAR-%05d", Math.max(1, number));
    }
}
