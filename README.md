# University Event Planning System

A comprehensive desktop application designed for managing university events and participants. Built with Java Swing, this system features a secure login, participant management, and local data persistence.

## Features
- **User Authentication**: Secure login dialog with session management.
- **Event Management**: Create and track university events.
- **Participant Tracking**: Manage attendee details and records.
- **Data Persistence**: Local database integration via `DatabaseHelper`.
- **Custom UI**: Includes a splash screen and basic theming support.

## Project Structure
The project is organized as follows:
- `src/eventmanager/`: Source code package.
  - `Main.java`: Entry point.
  - `EventManagerFrame.java`: Primary GUI dashboard.
  - `DatabaseHelper.java`: Database connectivity and logic.
  - `Participant.java` & `UniversityEvent.java`: Data models.
- `data/`: Configuration files (e.g., `settings.properties`).

## Prerequisites
- **Java Development Kit (JDK) 8** or higher.
- (Optional) IntelliJ IDEA or Eclipse for development.

## Getting Started

### 1. Compilation
Open a terminal in the project root and run:

```powershell
# Create output directory
mkdir bin

# Compile sources
javac -d bin src/eventmanager/*.java
```

### 2. Running the Application
Start the application using:

```powershell
java -cp bin eventmanager.Main
```

## Configuration
Application settings can be found in `data/settings.properties`. You can adjust theme settings and other runtime configurations there.

## License
**Academic Work**
This software is developed for academic purposes. It is intended for educational use and evaluation.

## Troubleshooting
- **Class not found?** Ensure you run the command from the project root and include the `-cp bin` flag.
- **Database issues?** Check `data/settings.properties` and ensure you have write permissions in the directory.
