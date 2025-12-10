package eventmanager;

import java.io.Serializable;

/**
 * Represents a participant registered for an event.
 */
public class Participant implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String participantId;
    private final String fullName;
    private final ParticipantType type;

    public Participant(String participantId, String fullName, ParticipantType type) {
        this.participantId = participantId;
        this.fullName = fullName;
        this.type = type;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getFullName() {
        return fullName;
    }

    public ParticipantType getType() {
        return type;
    }

    public enum ParticipantType {
        STUDENT,
        STAFF
    }
}

