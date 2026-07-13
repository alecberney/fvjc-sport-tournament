package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupGenerateRequest;
import abe.fvjc.tournament.domain.group.GroupSwapRequest;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomGroupId;

@UtilityClass
public class GroupFakes {

    public static Group buildGroup(final TournamentId tournamentId) {
        return Group.builder()
            .id(randomGroupId())
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
