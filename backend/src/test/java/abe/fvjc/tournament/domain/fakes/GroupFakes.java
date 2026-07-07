package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupGenerateRequest;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import abe.fvjc.tournament.domain.group.GroupSwapRequest;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomGroupId;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;

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

    public static GroupRanking buildRanking(final GroupId groupId, final String groupName, final int numEntries) {
        final var entries = new ArrayList<GroupRankingEntry>();
        for (int i = 1; i <= numEntries; i++) {
            final var entry = buildGroupRankingEntry(i, groupName + i, 10, 5, 5);
            entries.add(entry);
        }
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }

    private static GroupRankingEntry buildGroupRankingEntry(
            final int rank,
            final String groupName,
            final int goalsFor,
            final int goalsAgainst,
            final int goalsAgainst1) {
        return GroupRankingEntry.builder()
                .rank(rank)
                .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName))
                .goalsFor(goalsFor)
                .goalsAgainst(goalsAgainst)
                .goalDifference(goalsAgainst1)
                .build();
    }

    public static GroupRanking buildRankingWithExtras(final GroupId groupId, final String groupName) {
        final var entries = List.of(
                buildGroupRankingEntry(1, groupName + "1", 10, 5, 5),
                buildGroupRankingEntry(2, groupName + "2", 6, 8, -2));
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }
}
