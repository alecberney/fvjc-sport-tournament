package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.fakes.GroupFakes;
import abe.fvjc.tournament.domain.fakes.ScheduleFakes;
import abe.fvjc.tournament.domain.fakes.TeamFakes;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupRankingServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @InjectMocks
    private GroupRankingService groupRankingService;

    @Test
    void computeGroupRankingWhenNoResultsShouldReturnZeroStats() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(tournamentId);
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(null);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId, groupId);

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertEquals(2, rankingFound.getEntries().size());
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPlayed() == 0));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 0));
    }

    @Test
    void computeGroupRankingWhenWinShouldGive2Points() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(tournamentId);
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(3).score2(1).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId, groupId);

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        final var winner = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(t1.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(2, winner.getPoints());
        assertEquals(1, winner.getWins());
        assertEquals(1, winner.getRank());

        final var loser = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(t2.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(0, loser.getPoints());
        assertEquals(2, loser.getRank());
    }

    @Test
    void computeGroupRankingWhenDrawShouldGive1PointEach() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(tournamentId);
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(2).score2(2).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId, groupId);

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getDraws() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeGroupRankingWhenMultipleResultsShouldOrderByPointsThenGoalDiffThenGoalsFor() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(tournamentId);
        final var m1 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t1.getId()).withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(3).score2(0).build());
        final var m2 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t1.getId()).withTeam2Id(t3.getId())
                .withResult(MatchResult.builder().score1(1).score2(0).build());
        final var m3 = ScheduleFakes.buildMatch(round.getId()).withGroupId(groupId)
                .withTeam1Id(t2.getId()).withTeam2Id(t3.getId())
                .withResult(MatchResult.builder().score1(2).score2(0).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2, t3));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(m1, m2, m3));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId, groupId);

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertEquals(t1.getId().value(), rankingFound.getEntries().get(0).getTeam().getId().value());
        assertEquals(1, rankingFound.getEntries().get(0).getRank());
        assertEquals(t2.getId().value(), rankingFound.getEntries().get(1).getTeam().getId().value());
        assertEquals(2, rankingFound.getEntries().get(1).getRank());
        assertEquals(t3.getId().value(), rankingFound.getEntries().get(2).getTeam().getId().value());
        assertEquals(3, rankingFound.getEntries().get(2).getRank());
    }

    @Test
    void computeGroupRankingWhenFullyTiedShouldShareRank() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of());

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId, groupId);

        verify(teamStore).findAllByGroupId(groupId);

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeAllGroupRankingsWhenNoFilterShouldReturnAllGroups() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupIdA = GroupId.of(UUID.randomUUID());
        final var groupIdB = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");
        final var groupB = GroupFakes.buildGroup(null).withId(groupIdB).withName("B");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupIdA);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupIdB);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
        when(teamStore.findAllByGroupId(groupIdA)).thenReturn(List.of(t1));
        when(teamStore.findAllByGroupId(groupIdB)).thenReturn(List.of(t2));
        when(matchStore.findAllByGroupId(groupIdA)).thenReturn(List.of());
        when(matchStore.findAllByGroupId(groupIdB)).thenReturn(List.of());

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId, List.of());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupIdA);
        verify(teamStore).findAllByGroupId(groupIdB);
        verify(matchStore).findAllByGroupId(groupIdA);
        verify(matchStore).findAllByGroupId(groupIdB);

        assertEquals(2, rankingsFound.size());
    }

    @Test
    void computeAllGroupRankingsWhenFilterAppliedShouldReturnMatchingGroups() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupIdA = GroupId.of(UUID.randomUUID());
        final var groupIdB = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");
        final var groupB = GroupFakes.buildGroup(null).withId(groupIdB).withName("B");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupIdA);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
        when(teamStore.findAllByGroupId(groupIdA)).thenReturn(List.of(t1));
        when(matchStore.findAllByGroupId(groupIdA)).thenReturn(List.of());

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId, List.of("A"));

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupIdA);
        verify(matchStore).findAllByGroupId(groupIdA);

        assertEquals(1, rankingsFound.size());
        assertEquals("A", rankingsFound.get(0).getGroupName());
    }

    @Test
    void computeAllGroupRankingsWhenFilterMatchesNoneShouldReturnEmptyList() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var groupIdA = GroupId.of(UUID.randomUUID());
        final var groupA = GroupFakes.buildGroup(null).withId(groupIdA).withName("A");

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA));

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId, List.of("Z"));

        verify(groupStore).findAllByTournamentId(tournamentId);

        assertEquals(0, rankingsFound.size());
    }
}
