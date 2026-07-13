package abe.fvjc.tournament.domain.bracket;

import java.util.UUID;

public record BracketMatchId(UUID value) {

    public static BracketMatchId of(final UUID value) {
        return new BracketMatchId(value);
    }

    public static BracketMatchId empty() {
        return new BracketMatchId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
