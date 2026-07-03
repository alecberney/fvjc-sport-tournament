package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static GroupId groupId() {
        return GroupId.of(UUID.randomUUID());
    }

    static TournamentId tournamentId() {
        return TournamentId.of(UUID.randomUUID());
    }
}
