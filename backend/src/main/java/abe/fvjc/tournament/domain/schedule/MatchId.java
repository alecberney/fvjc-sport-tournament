package abe.fvjc.tournament.schedule.domain;

import java.util.UUID;

public record MatchId(UUID value) {

    public static MatchId of(final UUID value) {
        return new MatchId(value);
    }

    public static MatchId empty() {
        return new MatchId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
