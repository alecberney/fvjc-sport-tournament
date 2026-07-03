package abe.fvjc.tournament.schedule.domain;

import java.util.UUID;

public record RoundId(UUID value) {

    public static RoundId of(final UUID value) {
        return new RoundId(value);
    }

    public static RoundId empty() {
        return new RoundId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
