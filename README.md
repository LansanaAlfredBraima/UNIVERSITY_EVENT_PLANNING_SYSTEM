# Event Manager (Java Swing)

A simple desktop Event Manager application implemented in Java (package `eventmanager`). The app provides a GUI for event and participant management with a login flow, splash screen, basic theming, and a local database helper. This repository contains the source code for the project.

**Project**: Java Swing Event Manager

**Key files**:
- `src/eventmanager/Main.java`: Application entry point.
- `src/eventmanager/EventManagerFrame.java`: Main application window.
- `src/eventmanager/LoginDialog.java`: Login dialog.
- `src/eventmanager/SplashScreen.java`: Startup splash screen.
- `src/eventmanager/DatabaseHelper.java`: Database access helper (see implementation for DB details).
- `src/eventmanager/Participant.java`: Participant model.
- `src/eventmanager/UniversityEvent.java`: Event model.
- `src/eventmanager/Theme.java`: Theme and UI helper.
- `data/settings.properties`: Application configuration file.

**Features**
- Login screen and session entry.
- Splash screen on startup.
- Event and participant model classes.
- Local database helper for persistence (check `DatabaseHelper.java`).
- Simple theming support via `Theme.java`.

**Requirements**
- Java 8 or newer (JDK installed).
- A Java IDE is recommended (IntelliJ IDEA, Eclipse) or the JDK command-line tools.

**Build & Run (command-line)**

Open a PowerShell prompt at the project root (folder that contains `src` and `data`) and run:

```powershell
# Create an output directory for compiled classes
mkdir bin

# Compile all Java sources into the bin directory
javac -d bin src/eventmanager/*.java

# Run the application
java -cp bin eventmanager.Main
```

Notes:
- If the package or folder layout changes, adjust the `javac`/`java` commands accordingly.
- Using an IDE will simplify compilation and running; import the project as a Java project and run `eventmanager.Main`.

**Configuration**
- Application settings are read from `data/settings.properties`.
- Edit this file to change runtime settings (default location for DB, UI options, etc.). Check `DatabaseHelper.java` for database path/name used by the app.

**Project Structure**
- `src/eventmanager` — Java source files (package `eventmanager`).
- `data` — runtime configuration files (e.g., `settings.properties`).

**Troubleshooting**
- "Class not found" or "NoClassDefFoundError": ensure `javac` succeeded and you run `java` with `-cp bin` and the correct package-qualified main class: `eventmanager.Main`.
- Database errors: inspect `DatabaseHelper.java` and `data/settings.properties` for the DB file path and permissions.
- UI issues: check console output for stack traces; run from an IDE to get richer debugging.

**Contributing**
- Feel free to open issues or submit pull requests. For larger changes, please open an issue first to discuss the proposed change.

**License**
- This project does not include a formal license. Add one if you intend to share or distribute the code.

**Contact / Questions**
- For questions or help running the project, open an issue in the repo or contact the project owner directly.
