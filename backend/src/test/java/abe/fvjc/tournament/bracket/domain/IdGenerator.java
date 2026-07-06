package abe.fvjc.tournament.bracket.domain;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static BracketRoundId roundId() {
        return BracketRoundId.of(UUID.randomUUID());
    }
    static BracketMatchId matchId() {
        return BracketMatchId.of(UUID.randomUUID());
    }
}
