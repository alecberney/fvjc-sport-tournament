package abe.fvjc.tournament.tournament.domain;

import java.util.UUID;

public record TournamentId(UUID value) {

    public static TournamentId of(UUID value) {
        return new TournamentId(value);
    }

    public static TournamentId empty() {
        return new TournamentId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
