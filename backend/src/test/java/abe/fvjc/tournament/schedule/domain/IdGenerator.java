package abe.fvjc.tournament.schedule.domain;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static RoundId roundId() {
        return RoundId.of(UUID.randomUUID());
    }

    static MatchId matchId() {
        return MatchId.of(UUID.randomUUID());
    }
}
