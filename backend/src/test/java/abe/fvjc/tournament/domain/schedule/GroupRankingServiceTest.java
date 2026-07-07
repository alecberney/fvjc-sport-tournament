package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.team.TeamStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static abe.fvjc.tournament.domain.fakes.MatchFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;

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
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(null);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team2 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var round = buildRound(tournamentId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team2.getId())
                .withResult(null);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId.value(), groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertEquals(2, rankingFound.getEntries().size());
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPlayed() == 0));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 0));
    }

    @Test
    void computeGroupRankingWhenWinShouldGive2Points() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(null);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team2 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var round = buildRound(tournamentId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team2.getId())
                .withResult(MatchResult.builder().score1(3).score2(1).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId.value(), groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        final var winner = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(team1.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(2, winner.getPoints());
        assertEquals(1, winner.getWins());
        assertEquals(1, winner.getRank());

        final var loser = rankingFound.getEntries().stream()
                .filter(e -> e.getTeam().getId().value().equals(team2.getId().value()))
                .findFirst().orElseThrow();
        assertEquals(0, loser.getPoints());
        assertEquals(2, loser.getRank());
    }

    @Test
    void computeGroupRankingWhenDrawShouldGive1PointEach() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(null);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team2 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var round = buildRound(tournamentId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team2.getId())
                .withResult(MatchResult.builder().score1(2).score2(2).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId.value(), groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getPoints() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getDraws() == 1));
        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeGroupRankingWhenMultipleResultsShouldOrderByPointsThenGoalDiffThenGoalsFor() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(null);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team2 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team3 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var round = buildRound(tournamentId);
        final var match1 = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team2.getId())
                .withResult(MatchResult.builder().score1(3).score2(0).build());
        final var match2 = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team3.getId())
                .withResult(MatchResult.builder().score1(1).score2(0).build());
        final var match3 = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team2.getId())
                .withTeam2Id(team3.getId())
                .withResult(MatchResult.builder().score1(2).score2(0).build());

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2, team3));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of(match1, match2, match3));

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId.value(), groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertEquals(team1.getId().value(), rankingFound.getEntries().get(0).getTeam().getId().value());
        assertEquals(1, rankingFound.getEntries().get(0).getRank());
        assertEquals(team2.getId().value(), rankingFound.getEntries().get(1).getTeam().getId().value());
        assertEquals(2, rankingFound.getEntries().get(1).getRank());
        assertEquals(team3.getId().value(), rankingFound.getEntries().get(2).getTeam().getId().value());
        assertEquals(3, rankingFound.getEntries().get(2).getRank());
    }

    @Test
    void computeGroupRankingWhenFullyTiedShouldShareRank() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(null);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, null)
                .withGroupId(groupId);
        final var team2 = buildTeam(organisationId, null)
                .withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2));
        when(matchStore.findAllByGroupId(groupId)).thenReturn(List.of());

        final var rankingFound = groupRankingService.computeGroupRanking(tournamentId.value(), groupId.value());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);
        verify(matchStore).findAllByGroupId(groupId);

        assertTrue(rankingFound.getEntries().stream().allMatch(e -> e.getRank() == 1));
    }

    @Test
    void computeAllGroupRankingsWhenNoFilterShouldReturnAllGroups() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var groupA = buildGroup(null).withName("A");
        final var groupB = buildGroup(null).withName("B");
        final var groupIdA = groupA.getId();
        final var groupIdB = groupB.getId();
        final var team1 = buildTeam(organisationId, null).withGroupId(groupIdA);
        final var team2 = buildTeam(organisationId, null).withGroupId(groupIdB);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
        when(teamStore.findAllByGroupId(groupIdA)).thenReturn(List.of(team1));
        when(teamStore.findAllByGroupId(groupIdB)).thenReturn(List.of(team2));
        when(matchStore.findAllByGroupId(groupIdA)).thenReturn(List.of());
        when(matchStore.findAllByGroupId(groupIdB)).thenReturn(List.of());

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId.value(), List.of());

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupIdA);
        verify(teamStore).findAllByGroupId(groupIdB);
        verify(matchStore).findAllByGroupId(groupIdA);
        verify(matchStore).findAllByGroupId(groupIdB);

        assertEquals(2, rankingsFound.size());
    }

    @Test
    void computeAllGroupRankingsWhenFilterAppliedShouldReturnMatchingGroups() {
        final var tournamentId = buildTournament().getId();
        final var organisationId = buildOrganisation().getId();
        final var groupA = buildGroup(null).withName("A");
        final var groupB = buildGroup(null).withName("B");
        final var groupIdA = groupA.getId();
        final var team1 = buildTeam(organisationId, null).withGroupId(groupIdA);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB));
        when(teamStore.findAllByGroupId(groupIdA)).thenReturn(List.of(team1));
        when(matchStore.findAllByGroupId(groupIdA)).thenReturn(List.of());

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId.value(), List.of("A"));

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupIdA);
        verify(matchStore).findAllByGroupId(groupIdA);

        assertEquals(1, rankingsFound.size());
        assertEquals("A", rankingsFound.get(0).getGroupName());
    }

    @Test
    void computeAllGroupRankingsWhenFilterMatchesNoneShouldReturnEmptyList() {
        final var tournamentId = buildTournament().getId();
        final var groupA = buildGroup(null).withName("A");

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA));

        final var rankingsFound = groupRankingService.computeAllGroupRankings(tournamentId.value(), List.of("Z"));

        verify(groupStore).findAllByTournamentId(tournamentId);

        assertEquals(0, rankingsFound.size());
    }
}
