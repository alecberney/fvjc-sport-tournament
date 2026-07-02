package abe.fvjc.tournament.tournament.domain;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static TournamentId tournamentId() {
        return TournamentId.of(UUID.randomUUID());
    }
}
