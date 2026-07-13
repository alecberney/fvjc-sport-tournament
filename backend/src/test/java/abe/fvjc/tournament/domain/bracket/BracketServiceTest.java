package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.fakes.BracketFakes;
import abe.fvjc.tournament.domain.fakes.GroupFakes;
import abe.fvjc.tournament.domain.fakes.TournamentFakes;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import abe.fvjc.tournament.domain.schedule.MatchResult;
import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private BracketMatchStore bracketMatchStore;

    @Mock
    private BracketRoundStore bracketRoundStore;

    @Mock
    private GroupStore groupStore;

    @Mock
    private BracketMatchSearchService bracketMatchSearchService;

    @Mock
    private GroupRankingService groupRankingService;

    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void generateWhenValidRequestShouldCreateRoundsAndMatches() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequest();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");
        final var groups = List.of(gA, gB, gC, gD);

        final var rankings = List.of(
                buildRanking(gA.getId(), "A", 2),
                buildRanking(gB.getId(), "B", 2),
                buildRanking(gC.getId(), "C", 2),
                buildRanking(gD.getId(), "D", 2));

        when(tournamentSearchService.findById(tid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tid)).thenReturn(groups);
        when(groupRankingService.computeAllGroupRankings(tid, List.of())).thenReturn(rankings);
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tid, request);

        verify(tournamentSearchService).findById(tid);
        verify(groupStore).findAllByTournamentId(tid);
        verify(groupRankingService).computeAllGroupRankings(tid, List.of());
        verify(bracketRoundStore, times(4)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(8)).save(any(BracketMatch.class));

        assertEquals(4, roundsGenerated.size());
        assertEquals("Quarts de finale", roundsGenerated.get(0).getName());
        assertEquals("Demi-finales", roundsGenerated.get(1).getName());
        assertEquals("Finale", roundsGenerated.get(2).getName());
        assertEquals("Troisième place", roundsGenerated.get(3).getName());
        assertEquals(4, roundsGenerated.get(0).getMatches().size());
        assertEquals(2, roundsGenerated.get(1).getMatches().size());
        assertEquals(1, roundsGenerated.get(2).getMatches().size());
        assertEquals(1, roundsGenerated.get(3).getMatches().size());
    }

    @Test
    void generateWhenTotalTeamsNotPowerOfTwoShouldThrowValidationException() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestNotPowerOfTwo();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");

        when(tournamentSearchService.findById(tid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tid)).thenReturn(List.of(gA, gB, gC));
        when(groupRankingService.computeAllGroupRankings(tid, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 2),
                        buildRanking(gB.getId(), "B", 2),
                        buildRanking(gC.getId(), "C", 2)));

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.generate(tid, request));

        verify(tournamentSearchService).findById(tid);
        verify(groupStore).findAllByTournamentId(tid);
        verify(groupRankingService).computeAllGroupRankings(tid, List.of());

        assertEquals(1, exception.getErrors().size());
        assertEquals("totalQualifiedTeams", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenFourTeamsQualifiedShouldCreateTwoRounds() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestWithFourTeams();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");

        when(tournamentSearchService.findById(tid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tid)).thenReturn(List.of(gA, gB, gC, gD));
        when(groupRankingService.computeAllGroupRankings(tid, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 1),
                        buildRanking(gB.getId(), "B", 1),
                        buildRanking(gC.getId(), "C", 1),
                        buildRanking(gD.getId(), "D", 1)));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tid, request);

        verify(tournamentSearchService).findById(tid);
        verify(groupStore).findAllByTournamentId(tid);
        verify(groupRankingService).computeAllGroupRankings(tid, List.of());
        verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(4)).save(any(BracketMatch.class));

        assertEquals(3, roundsGenerated.size());
        assertEquals("Demi-finales", roundsGenerated.get(0).getName());
        assertEquals("Finale", roundsGenerated.get(1).getName());
        assertEquals("Troisième place", roundsGenerated.get(2).getName());
        assertEquals(2, roundsGenerated.get(0).getMatches().size());
        assertEquals(1, roundsGenerated.get(1).getMatches().size());
        assertEquals(1, roundsGenerated.get(2).getMatches().size());
    }

    @Test
    void generateWhenExtraQualifiersShouldSelectByTieBreakerAndCreateBracket() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestWithExtras();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");

        when(tournamentSearchService.findById(tid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tid)).thenReturn(List.of(gA, gB, gC));
        when(groupRankingService.computeAllGroupRankings(tid, List.of())).thenReturn(
                List.of(buildRankingWithExtras(gA.getId(), "A"),
                        buildRankingWithExtras(gB.getId(), "B"),
                        buildRankingWithExtras(gC.getId(), "C")));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tid, request);

        verify(tournamentSearchService).findById(tid);
        verify(groupStore).findAllByTournamentId(tid);
        verify(groupRankingService).computeAllGroupRankings(tid, List.of());
        verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(4)).save(any(BracketMatch.class));

        assertEquals(3, roundsGenerated.size());
        assertEquals("Demi-finales", roundsGenerated.get(0).getName());
        assertEquals("Finale", roundsGenerated.get(1).getName());
        assertEquals("Troisième place", roundsGenerated.get(2).getName());
        assertEquals(2, roundsGenerated.get(0).getMatches().size());
        assertEquals(1, roundsGenerated.get(1).getMatches().size());
        assertEquals(1, roundsGenerated.get(2).getMatches().size());
    }

    @Test
    void enterResultWhenTerminalMatchShouldSaveResultOnly() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var match = BracketFakes.buildMatch()
                .withId(matchId)
                .withNextMatchId(null)
                .withLoserNextMatchId(null);
        final var request = BracketFakes.buildMatchResultRequest();

        when(bracketMatchSearchService.findById(matchId)).thenReturn(match);
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId, request);

        verify(bracketMatchSearchService).findById(matchId);
        verify(bracketMatchStore).save(any(BracketMatch.class));

        assertNotNull(matchUpdated.getResult());
        assertEquals(3, matchUpdated.getResult().getScore1());
        assertEquals(1, matchUpdated.getResult().getScore2());
    }

    @Test
    void enterResultWhenHasNextMatchShouldAdvanceWinnerToNextMatch() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var nextMatch = BracketFakes.buildMatch().withTeam1(null).withTeam2(null);
        final var nextMatchId = nextMatch.getId();
        final var match = BracketFakes.buildMatch()
                .withId(matchId)
                .withNextMatchId(nextMatchId)
                .withNextMatchTeamSlot(1)
                .withLoserNextMatchId(null);
        final var request = BracketFakes.buildMatchResultRequest();

        when(bracketMatchSearchService.findById(matchId)).thenReturn(match);
        when(bracketMatchSearchService.findById(nextMatchId)).thenReturn(nextMatch);
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId, request);

        // score1(3) > score2(1) → team1 wins, slot=1 → lands in team1 of next match
        final var winner = match.getTeam1();
        verify(bracketMatchStore, times(2)).save(any(BracketMatch.class));
        verify(bracketMatchStore).save(argThat(m -> nextMatchId.equals(m.getId()) && winner.equals(m.getTeam1())));

        assertNotNull(matchUpdated.getResult());
    }

    @Test
    void enterResultWhenDemiFinaleLoserShouldAdvanceToTroisiemePlace() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var nextMatch = BracketFakes.buildMatch().withTeam1(null).withTeam2(null);
        final var loserMatch = BracketFakes.buildMatch().withTeam1(null).withTeam2(null);
        final var nextMatchId = nextMatch.getId();
        final var loserMatchId = loserMatch.getId();
        final var match = BracketFakes.buildMatch()
                .withId(matchId)
                .withNextMatchId(nextMatchId)
                .withNextMatchTeamSlot(2)
                .withLoserNextMatchId(loserMatchId)
                .withLoserNextMatchTeamSlot(2);
        final var request = BracketFakes.buildMatchResultRequest();

        when(bracketMatchSearchService.findById(matchId)).thenReturn(match);
        when(bracketMatchSearchService.findById(nextMatchId)).thenReturn(nextMatch);
        when(bracketMatchSearchService.findById(loserMatchId)).thenReturn(loserMatch);
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId, request);

        // score1(3) > score2(1) → team1 wins, team2 loses, slot=2 → team2 position
        final var winner = match.getTeam1();
        final var loser = match.getTeam2();
        verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));
        verify(bracketMatchStore).save(argThat(m -> nextMatchId.equals(m.getId()) && winner.equals(m.getTeam2())));
        verify(bracketMatchStore).save(argThat(m -> loserMatchId.equals(m.getId()) && loser.equals(m.getTeam2())));

        assertNotNull(matchUpdated.getResult());
    }

    @Test
    void enterResultWhenMatchAlreadyHasResultShouldUpdateResult() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var match = BracketFakes.buildMatch()
                .withId(matchId)
                .withNextMatchId(null)
                .withLoserNextMatchId(null)
                .withResult(MatchResult.builder().score1(1).score2(3).build());
        final var request = BracketMatchResultRequest.builder().score1(5).score2(2).build();

        when(bracketMatchSearchService.findById(matchId)).thenReturn(match);
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId, request);

        verify(bracketMatchSearchService).findById(matchId);
        verify(bracketMatchStore).save(any(BracketMatch.class));

        assertEquals(5, matchUpdated.getResult().getScore1());
        assertEquals(2, matchUpdated.getResult().getScore2());
    }

    @Test
    void enterResultWhenDrawShouldThrowValidationException() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var request = BracketMatchResultRequest.builder().score1(2).score2(2).build();

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.enterResult(matchId, request));

        assertEquals(1, exception.getErrors().size());
        assertEquals("scores", exception.getErrors().get(0).field());
    }

    @Test
    void enterResultWhenScoreNegativeShouldThrowValidationException() {
        final var matchId = BracketMatchId.of(UUID.randomUUID());
        final var request = BracketMatchResultRequest.builder().score1(-1).score2(2).build();

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.enterResult(matchId, request));

        assertEquals(1, exception.getErrors().size());
        assertEquals("scores", exception.getErrors().get(0).field());
    }

    @Test
    void deleteAllByTournamentIdShouldDeleteBracketMatchesThenRounds() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var round = BracketRound.builder()
                .id(BracketRoundId.of(UUID.randomUUID()))
                .build();

        when(bracketRoundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(round));

        bracketService.deleteAllByTournamentId(tournamentId);

        verify(bracketRoundStore).findAllByTournamentId(tournamentId);
        verify(bracketMatchStore).deleteAllByRoundId(round.getId());
        verify(bracketRoundStore).deleteAllByTournamentId(tournamentId);
    }

    private static GroupRanking buildRanking(final GroupId groupId, final String groupName, final int numEntries) {
        final var entries = new ArrayList<GroupRankingEntry>();
        for (int i = 1; i <= numEntries; i++) {
            entries.add(GroupRankingEntry.builder()
                    .rank(i)
                    .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + i))
                    .goalsFor(10)
                    .goalsAgainst(5)
                    .goalDifference(5)
                    .build());
        }
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }

    private static GroupRanking buildRankingWithExtras(final GroupId groupId, final String groupName) {
        final var entries = List.of(
                GroupRankingEntry.builder()
                        .rank(1)
                        .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + "1"))
                        .goalsFor(10)
                        .goalsAgainst(5)
                        .goalDifference(5)
                        .build(),
                GroupRankingEntry.builder()
                        .rank(2)
                        .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + "2"))
                        .goalsFor(6)
                        .goalsAgainst(8)
                        .goalDifference(-2)
                        .build());
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }
}
