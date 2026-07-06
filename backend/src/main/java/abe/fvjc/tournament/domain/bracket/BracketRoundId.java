package abe.fvjc.tournament.bracket.domain;

import java.util.UUID;

public record BracketRoundId(UUID value) {
    public static BracketRoundId of(final UUID value) {
        return new BracketRoundId(value);
    }
    public static BracketRoundId empty() {
        return new BracketRoundId(null);
    }
    public boolean isEmpty() {
        return value == null;
    }
}
