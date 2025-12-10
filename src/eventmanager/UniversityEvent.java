package eventmanager;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an event with its metadata and registered participants.
 */
public class UniversityEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventId;
    private String name;
    private LocalDate date;
    private LocalTime time;
    private String venue;
    private String organizer;
    private String category;
    private final List<Participant> participants = new ArrayList<>();

    public UniversityEvent(String eventId,
                           String name,
                           LocalDate date,
                           LocalTime time,
                           String venue,
                           String organizer,
                           String category) {
        this.eventId = eventId;
        this.name = name;
        this.date = date;
        this.time = time;
        this.venue = venue;
        this.organizer = organizer;
        this.category = category;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
    }

    public int getParticipantCount() {
        return participants.size();
    }
}

