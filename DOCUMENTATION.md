# Event Manager — Documentation

Comprehensive developer and user documentation for the University Event Manager Java desktop application (package `eventmanager`). This document describes the architecture, components, data model, build/run steps, configuration, and guidance for extending the application.

**Table of contents**
- Overview
- Quick start
  - Requirements
  - Build & run (command-line)
  - Run from an IDE
- Project structure and key classes
- Persistence and data format
- UI flow and features
- Validation, business rules and constraints
- Theming and configuration
- Notifications
- Concurrency & persistence considerations
- Known limitations and security notes
- Extending the app (suggestions)
- Troubleshooting & FAQ
- Contributing
- License

---

## Overview

Event Manager is a small Java Swing desktop application for scheduling university events, registering participants, and viewing simple reports and statistics. It is intended as a single-user, local desktop tool that stores data on the file system using Java object serialization.

Primary goals:
- Manage events (create, update, delete)
- Register participants per event
- Detect simple venue/time conflicts
- Provide small reports and basic notifications
- Lightweight, no external DB required

## Quick start

### Requirements
- Java Development Kit (JDK) 8 or newer installed and on the PATH.
- Optional: an IDE such as IntelliJ IDEA or Eclipse for easier editing and debugging.

### Build & run (command-line — PowerShell)

Open PowerShell at the project root (the folder that contains `src` and `data`) and run:

```powershell
# Create output folder for compiled classes
mkdir bin

# Compile all Java files into the bin directory
javac -d bin src/eventmanager/*.java

# Run the application
java -cp bin eventmanager.Main
```

Notes:
- If compilation fails due to Java version mismatches, install a matching JDK or adjust project language level in your IDE.
- To create a runnable JAR (optional), compile and then run `jar` to package classes and resources. Example minimal steps:

```powershell
# After compilation
jar --create --file event-manager.jar -C bin .
# Run jar
java -cp event-manager.jar eventmanager.Main
```

### Run from an IDE
- Import the folder as a Java project (IntelliJ/Eclipse).
- Ensure project SDK is set to JDK 8+.
- Run the `eventmanager.Main` class.

## Project structure and key classes

- `src/eventmanager/` — Java source files (package `eventmanager`).
  - `Main.java` — Application entry point. Applies theme, displays `SplashScreen`, shows `LoginDialog`, and opens `EventManagerFrame` on successful authentication.
  - `SplashScreen.java` — Simple startup screen shown for ~5 seconds.
  - `LoginDialog.java` — Modal login dialog (hardcoded default credentials: `Group1` / `admin123`).
  - `EventManagerFrame.java` — The main Swing application window; contains event list, participant list, editor pane and reports.
  - `DatabaseHelper.java` — Small persistence helper that serializes/deserializes a `List<UniversityEvent>` to `data/events.dat`.
  - `UniversityEvent.java` — Domain model for an event (ID, name, date, time, venue, organizer, category, participants).
  - `Participant.java` — Domain model for a participant; contains an enum `ParticipantType { STUDENT, STAFF }`.
  - `Theme.java` — Centralized UI palette and helper methods (themable colors, font, styles).

- `data/` — Application configuration and storage files.
  - `settings.properties` — Persisted UI/theme setting (`theme.dark=true|false`).
  - `events.dat` (created at runtime) — Serialized list of `UniversityEvent` objects used for persistence.

## Persistence and data format

The app uses a very lightweight persistence layer implemented in `DatabaseHelper`:
- Storage directory: `data/` (created automatically if missing).
- Data file: `data/events.dat` — stores a Java-serialized `List<UniversityEvent>`.
- Methods in `DatabaseHelper` are `synchronized` to reduce concurrent access issues within a single JVM.

Important details and implications:
- Java serialization is used (implements `Serializable` in `UniversityEvent` and `Participant`). This is simple but has drawbacks (not human-readable, fragile when classes change, potential security concerns when loading untrusted data).
- If `events.dat` is missing or cannot be read, the app gracefully initializes with an empty event list.
- To migrate to JSON/SQLite later, replace `DatabaseHelper` implementation (suggestions provided in "Extending the app").

## UI flow and features

Startup and login
- `Main` applies the theme, shows the `SplashScreen` for ~5 seconds, then presents `LoginDialog`.
- Login credentials are hardcoded in `LoginDialog` (`DEFAULT_USERNAME = "Group1"`, `DEFAULT_PASSWORD = "admin123"`). If authentication succeeds, `EventManagerFrame` is shown.

Main window (`EventManagerFrame`)
- Left/center: Table of scheduled events (sortable by date in code) and participants area.
- Right: Editor panel to add/update events and buttons for actions: Add Event, Update Event, Delete Event, Register Participant, Generate Reports.
- Reports dialog provides multiple report tabs: upcoming schedule, participant roster, statistics.

Event lifecycle
- Add Event: user supplies an `Event ID` (pattern `EVT-\d{4}`), selects name/venue/organizer from dropdowns, selects date/time. On adding:
  - Duplicate ID or name handling is interactive: dialogs offer auto-generation/auto-rename.
  - After adding, participant registration dialog opens automatically to register initial participants.
- Update Event: modifies selected event; shows a confirmation summary and conflict resolution dialogs when necessary.
- Delete Event: confirms via dialog.

Participant registration
- Per-event registration dialog allows adding multiple participants sequentially.
- Participant IDs are formatted `PAR-%05d` and are assigned per-event (ensuring uniqueness per event and global counter).
- Duplicate participant names for the same event are prevented.

Reports and insights
- Simple reporting: upcoming schedule, participant roster, and statistics (total events, total participants, busiest event).
- Basic conflict detection for venue/date/time (reports list clashes).

Notifications
- A scheduled background check runs periodically to surface notifications for events occurring soon.
- If the OS supports `SystemTray`, a tray notification is attempted; otherwise the status bar is updated.

## Validation, business rules and constraints

Key validation enforced by `EventManagerFrame.buildEventFromForm()`:
- `Event ID` is required and must match `EVT-\d{4}` (example `EVT-0001`).
- Event date/time cannot be in the past (today allowed but time must be in the future).
- Venue/time clash detection: events with same venue and exact same date/time are considered conflicting (prevents creation/update).
- Participant names for the same event must be unique.

## Theming and configuration

- Theme is centralized in `Theme.java` (colors, fonts, helpers).
- The status of dark mode is persisted to `data/settings.properties` via `Theme.saveSettings`/`Theme.loadSettings`.
- Example content of `data/settings.properties`:

```properties
#app settings
#Thu Nov 20 08:07:35 GMT 2025
theme.dark=false
```

- The UI exposes a toggle in the status bar to switch between light/dark themes. Toggling writes `settings.properties`.

## Notifications & scheduling

- The `EventManagerFrame` starts a scheduled executor that checks every minute for events happening within a 10-minute window. When a qualifying event is found, it attempts to show a system tray notification or a status message.
- The scheduler runs in the same JVM (`ScheduledExecutorService`), and notified event IDs are tracked to avoid duplicate notifications.

## Concurrency & persistence considerations

- `DatabaseHelper` methods are synchronized to avoid concurrent read/write races inside the same JVM process.
- The app is single-user, single-process — it is not safe for concurrent multi-process access to `data/events.dat` because Java serialization and file writes are not coordinated across processes.
- Suggested safer approaches for multi-process or networked scenarios:
  - Migrate persistence to a lightweight database like SQLite (via JDBC), which supports safe concurrent access.
  - Or use a JSON file and an external file-locking strategy.

## Known limitations and security notes

- Credentials are hardcoded in `LoginDialog`. This is acceptable for demo/learning usage but not for production. Replace with a secure authentication mechanism if needed.
- Java serialization deserialization of untrusted data can be a security risk. Do not share `events.dat` with untrusted sources. If you accept files from others, migrate to a safer format (JSON) or apply serialization whitelists.
- No encryption is used. If storing sensitive data, consider encrypting the data file or moving to a secured DB and enforcing filesystem ACLs.
- The event ID/participant ID generation is purely numeric and string-based; there is no central ID registry beyond what's stored in memory + serialized file.

## Extending the app (suggestions & minimal steps)

1. Replace serialization with JSON (Jackson / Gson)
   - Create a new `DatabaseHelperJson` implementing the same public methods (`loadEvents`, `saveEvents`, `findEventById`, `hasConflictingEvent`).
   - Serialize `List<UniversityEvent>` to `data/events.json` with a stable schema. Update `Main` to use the new helper.

2. Use SQLite (recommended for sturdier persistence)
   - Add a JDBC dependency and replace `DatabaseHelper` with SQL-based CRUD for events and participants.
   - Map `UniversityEvent` → `events` table, `Participant` → `participants` table with FK to `events`.

3. Export/Import features
   - Add menu items to export events to CSV/JSON and import from such files.

4. Unit and UI tests
   - Add JUnit tests for model and helper classes.
   - For Swing UI, use AssertJ-Swing or similar for basic UI automation tests.

5. Improve authentication
   - Replace hardcoded credentials with a user store (file-based encrypted password hashes or connect to an auth provider).

6. Add undo/redo
   - Implement a command pattern for actions (add/update/delete) so users can undo recent changes.

## Troubleshooting & FAQ

Q: The app fails to start with "ClassNotFoundException" or "NoClassDefFoundError".
- Confirm `javac` succeeded and you used `-d bin` to output class files.
- Run the app with `java -cp bin eventmanager.Main` from the project root.

Q: Events are not persisted between runs.
- Check `data/events.dat` exists and is writable by the user running the app.
- If the file is corrupted, the app will start with an empty list; you may restore from a `events.dat` backup or switch to JSON persistence.

Q: I changed class structure and now `events.dat` fails to load.
- Java serialization is sensitive to class changes. If you refactor model fields, existing `events.dat` may become unreadable. Consider migrating data by writing a small converter that reads the old format and writes the new format (or switch to JSON and reconstruct objects manually).

Q: I want to change the default login credentials.
- Edit `src/eventmanager/LoginDialog.java`, change `DEFAULT_USERNAME` and `DEFAULT_PASSWORD` constants, rebuild.

## Contributing

- Fork the project and open a pull request.
- For significant changes (migration to DB, API additions), open an issue to discuss the design first.
- Keep behavior backwards-compatible if possible (or provide a migration tool for serialized data).

## License

This repository does not include a formal license. Add an appropriate license file (e.g., `LICENSE`) if you intend to open-source or distribute the project.

---

If you want, I can:
- Convert `events.dat` persistence to JSON and provide a migration helper.
- Add a small `build-and-run.ps1` PowerShell script that compiles and runs the app.
- Generate example screenshots or a short user guide with annotated UI images.

Tell me which of these you'd like next and I will implement it.  

## **User Manual**

This user manual explains how to use the Event Manager application from a user's perspective. It assumes you have followed the Quick Start section and can run the application.

### **1. Launching the application**

- Open PowerShell at the project root (the folder that contains `src` and `data`).
- Compile and run using:

```powershell
mkdir bin
javac -d bin src/eventmanager/*.java
java -cp bin eventmanager.Main
```

- Or run `eventmanager.Main` from your IDE (IntelliJ, Eclipse) after importing the project and setting the SDK to JDK 8+.

When the app launches you will see a splash screen for a few seconds, then the login dialog.

### **2. Logging in**

- Default credentials (for demo purposes):
   - Username: `Group1`
   - Password: `admin123`
- Enter these credentials in the `Coordinator Login` dialog and press `Login`.
- If authentication fails, a message will show. Use the correct credentials and try again or edit `src/eventmanager/LoginDialog.java` to change defaults.

### **3. Main window overview**

The main application window (`University Events Dashboard`) has several regions:
- Header/hero: Title, subtitle, and quick metrics (Events, Participants).
- Center: Scheduled Events table — shows `Event ID`, `Name`, `Date & Time`, `Venue`, `Organizer`, `Category`, `Participants`.
- Bottom of center: Participant list for the currently selected event.
- Right: Editor panel where you can add or edit event details and action buttons.
- Status bar (bottom): Current status messages and a `Dark` toggle to switch themes.

General UI tips:
- Select an event in the table to populate the editor form and load participants.
- Use the status bar messages for quick feedback after actions.

### **4. Creating a new event**

Steps:
1. In the editor panel (right), enter/verify the `Event ID` in the format `EVT-0001` (the app auto-populates a suggested ID).
2. Choose the `Name` from the dropdown. The app provides sensible default event names.
3. Select `Date` using the date control and `Time` using the time spinner.
4. Choose `Venue` and `Organizer` from the dropdowns.
5. The `Category` is auto-selected from the chosen name; it is not editable directly.
6. Click `Add Event`.

Behavior and validation:
- `Event ID` must match the pattern `EVT-\d{4}`. If it is missing or malformed, the app shows an error.
- Date/time cannot be in the past. The same-day event is allowed but the time must be in the future.
- If the chosen `Venue` already has an event scheduled at the same date and time, the app will block creation and show an error about a clash.
- If an event with the same ID or name already exists, the app offers interactive options to auto-generate a new ID or auto-rename the name with a numeric suffix.

After a successful add:
- The event appears in the central table.
- The participant registration dialog opens automatically so you can add initial participants.

### **5. Updating an existing event**

Steps:
1. Select the event in the event table.
2. Edit fields in the editor panel. You may change `Event ID`, `Name`, `Date`, `Time`, `Venue`, `Organizer`.
3. Click `Update Event`.

Behavior and safety checks:
- The app shows a summary of the proposed changes and requests confirmation.
- If the new ID or new name conflicts with another event, it offers to auto-resolve (auto-generate ID or auto-rename) or cancel.
- All validation rules that apply during creation apply during updates as well (ID format, future date/time, no venue/time clash).

### **6. Deleting an event**

Steps:
1. Select the event in the table.
2. Click `Delete Event`.
3. Confirm deletion in the dialog.

Behavior:
- Deletion removes the event and its participant list from the in-memory data and persists the updated list to disk.
- There is no built-in undo — consider exporting or backing up `data/events.dat` before bulk deletions.

### **7. Registering participants**

To register people for an event:
1. Select an event in the table.
2. Click `Register Participant`.
3. In the modal dialog, enter the participant's full name and select `Type` (`STUDENT` or `STAFF`).
4. Click `Add` to register multiple participants sequentially, or `Done` to close the dialog.

Details:
- Participant IDs are generated per-event in the format `PAR-00001`.
- Duplicate participant names for the same event are blocked.
- Each added participant is persisted immediately.

### **8. Reports & Insights**

Click `Generate Reports` to open a dialog containing:
- `Upcoming Schedule` — Table of upcoming events with categories and participant counts.
- `Participant Roster` — Flattened list of participants and their event assignments.
- `Statistics` — Cards showing totals (events, participants) and a list of detected date/venue conflicts.

Use these views to quickly assess schedule density and popular events.

### **9. Theming**

- Toggle the `Dark` switch in the status bar to switch between light and dark themes.
- Theme preference is written to `data/settings.properties` and loaded on the next app start.

### **10. Notifications**

- The app runs a periodic check and will notify you of events occurring within the next 10 minutes.
- If your OS supports the system tray, a native tray notification will appear; otherwise the status bar shows the message.

### **11. Backups, Export & Restore (recommended workflows)**

Because the app uses Java serialization for persistence, it's a good practice to keep backups.

- Backup:
   - Copy `data/events.dat` to a safe location regularly (e.g., `events-backup-YYYYMMDD.dat`).

- Export (manual approach):
   - For portability, consider creating a small export utility that reads `data/events.dat` and writes JSON or CSV. (See "Extending the app" for suggestions.)

- Restore:
   - Close the application.
   - Replace `data/events.dat` with a backed-up file of compatible format (be cautious about serialization compatibility after code changes).

### **12. Keyboard shortcuts and accessibility**

- In the participant registration dialog:
   - `Enter` = Add participant
   - `Esc` = Done / close dialog
- General: focus traversal uses native Swing behavior — Tab to move between controls.

### **13. Troubleshooting (common issues and fixes)**

- Problem: "NoClassDefFoundError" or app does not start
   - Verify compilation: `javac -d bin src/eventmanager/*.java` completed without errors.
   - Run with `java -cp bin eventmanager.Main` from the project root.

- Problem: Events do not persist between runs
   - Verify the `data` directory contains `events.dat` and that the file is not read-only.
   - Check the application console or status bar for write errors.

- Problem: `events.dat` failing to load after code changes
   - Java serialized format is sensitive to class changes. If you refactored model classes, older `events.dat` may not deserialize.
   - If you still have an older runnable copy, run the old code and export to JSON/CSV, then import with the new code.

- Problem: Wrong login credentials
   - Edit `src/eventmanager/LoginDialog.java` and change `DEFAULT_USERNAME` / `DEFAULT_PASSWORD` values; rebuild.

### **14. Example user flows**

- Quick create & register flow:
   1. Click `Add Event` (accept auto-generated `Event ID`).
 2. Choose an event `Name`, `Date`, `Time`, `Venue`, `Organizer`.
 3. Confirm the event created, then use the participant dialog to add students/staff.

- Fixing a conflicting event:
   1. The app will prevent creating an event with an exact venue/date/time clash.
   2. Instead, pick a different `Time` or `Venue`. Use `Generate Reports` → `Statistics` to review clashes.