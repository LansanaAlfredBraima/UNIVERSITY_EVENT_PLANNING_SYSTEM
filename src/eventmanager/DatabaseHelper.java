package eventmanager;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lightweight persistence layer that stores events on disk using Java serialization.
 */
public class DatabaseHelper {
    private static final String DATA_DIRECTORY = "data";
    private static final String DATA_FILE = DATA_DIRECTORY + "/events.dat";

    public DatabaseHelper() {
        ensureStoragePresent();
    }

    private void ensureStoragePresent() {
        try {
            Files.createDirectories(Path.of(DATA_DIRECTORY));
            Path filePath = Path.of(DATA_FILE);
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
                saveEvents(new ArrayList<>());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to bootstrap local storage", e);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized List<UniversityEvent> loadEvents() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            Object data = ois.readObject();
            if (data instanceof List<?>) {
                return new ArrayList<>((List<UniversityEvent>) data);
            }
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            // If the file is empty or corrupted, start with a blank slate.
            return new ArrayList<>();
        }
    }

    public synchronized void saveEvents(List<UniversityEvent> events) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(events);
            oos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save events", e);
        }
    }

    public synchronized Optional<UniversityEvent> findEventById(String eventId) {
        return loadEvents().stream()
                .filter(ev -> ev.getEventId().equalsIgnoreCase(eventId))
                .findFirst();
    }

    public synchronized boolean hasConflictingEvent(String eventId, java.time.LocalDate date, String venue) {
        return loadEvents().stream()
                .filter(ev -> !ev.getEventId().equalsIgnoreCase(eventId))
                .anyMatch(ev -> ev.getVenue().equalsIgnoreCase(venue)
                        && ev.getDate().equals(date));
    }
}

