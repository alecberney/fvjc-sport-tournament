package abe.fvjc.tournament.team.domain;

import java.util.UUID;

public record TeamId(UUID value) {

    public static TeamId of(final UUID value) {
        return new TeamId(value);
    }

    public static TeamId empty() {
        return new TeamId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
