package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class GroupFakes {

    public static Group buildGroup(final TournamentId tournamentId) {
        return Group.builder()
            .id(IdGenerator.groupId())
            .name("A")
            .tournamentId(tournamentId)
            .build();
    }

    public static GroupGenerateRequest buildGenerateRequest() {
        return GroupGenerateRequest.builder()
            .groupSize(4)
            .build();
    }

    public static GroupSwapRequest buildSwapRequest(final UUID teamId1, final UUID teamId2) {
        return GroupSwapRequest.builder()
            .teamId1(teamId1)
            .teamId2(teamId2)
            .build();
    }
}
