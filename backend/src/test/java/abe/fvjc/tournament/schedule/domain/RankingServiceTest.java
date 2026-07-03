package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @InjectMocks
    private RankingService rankingService;

    @Test
    void computeGroupRankingWhenNoResultsShouldReturnZeroStats() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(null);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

        assertEquals(2, rankingFound.getEntries().size());
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPlayed() == 0));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 0));
    }

    @Test
    void computeGroupRankingWhenWinShouldGive2Points() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(3).score2(1).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

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
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
        final var match = ScheduleFakes.buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId())
                .withResult(MatchResult.builder().score1(2).score2(2).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(match));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getDraws() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeGroupRankingWhenMultipleResultsShouldOrderByPointsThenGoalDiffThenGoalsFor() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var round = ScheduleFakes.buildRound(abe.fvjc.tournament.tournament.domain.TournamentId.of(tournamentId));
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
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2, t3));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of(m1, m2, m3));

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId.value());
        verify(matchStore).findAllByGroupId(groupId.value());

        assertEquals(t1.getId().value(), rankingFound.getEntries().get(0).getTeam().getId().value());
        assertEquals(1, rankingFound.getEntries().get(0).getRank());
        assertEquals(t2.getId().value(), rankingFound.getEntries().get(1).getTeam().getId().value());
        assertEquals(2, rankingFound.getEntries().get(1).getRank());
        assertEquals(t3.getId().value(), rankingFound.getEntries().get(2).getTeam().getId().value());
        assertEquals(3, rankingFound.getEntries().get(2).getRank());
    }

    @Test
    void computeGroupRankingWhenFullyTiedShouldShareRank() {
        final var tournamentId = UUID.randomUUID();
        final var groupId = GroupId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(null).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, null).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, null).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));
        when(matchStore.findAllByGroupId(groupId.value())).thenReturn(List.of());

        final var rankingFound = rankingService.computeGroupRanking(tournamentId, groupId.value());

        verify(teamStore).findAllByGroupId(groupId.value());

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }
}
