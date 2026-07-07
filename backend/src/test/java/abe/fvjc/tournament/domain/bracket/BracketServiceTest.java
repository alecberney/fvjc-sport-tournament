package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;

import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildMatchResultRequest;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildTiedBracketMatchResultRequest;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildRanking;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildRankingWithExtras;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.MatchFakes.buildMatchResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGenerateRequestNotPowerOfTwo;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGenerateRequestWithExtras;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGenerateRequestWithFourTeams;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {
    @Mock
    private BracketMatchStore bracketMatchStore;
    @Mock
    private BracketRoundStore bracketRoundStore;
    @Mock
    private GroupStore groupStore;
    @Mock
    private GroupRankingService groupRankingService;
    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void generateWhenValidRequestShouldCreateRoundsAndMatches() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildGenerateRequest();
        final var groupA = buildGroup(tournamentId)
                .withName("A");
        final var groupB = buildGroup(tournamentId)
                .withName("B");
        final var groupC = buildGroup(tournamentId)
                .withName("C");
        final var groupD = buildGroup(tournamentId)
                .withName("D");
        final var groups = List.of(groupA, groupB, groupC, groupD);

        final var rankings = List.of(
                buildRanking(groupA.getId(), "A", 2),
                buildRanking(groupB.getId(), "B", 2),
                buildRanking(groupC.getId(), "C", 2),
                buildRanking(groupD.getId(), "D", 2));

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(groups);
        when(groupRankingService.computeAllGroupRankings(tournamentUuid, List.of())).thenReturn(rankings);
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(groupRankingService).computeAllGroupRankings(tournamentUuid, List.of());
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
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildGenerateRequestNotPowerOfTwo();

        final var groupA = buildGroup(tournamentId).withName("A");
        final var groupB = buildGroup(tournamentId).withName("B");
        final var groupC = buildGroup(tournamentId).withName("C");

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB, groupC));
        when(groupRankingService.computeAllGroupRankings(tournamentUuid, List.of())).thenReturn(
                List.of(buildRanking(groupA.getId(), "A", 2),
                        buildRanking(groupB.getId(), "B", 2),
                        buildRanking(groupC.getId(), "C", 2)));

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(groupRankingService).computeAllGroupRankings(tournamentUuid, List.of());

        assertEquals(1, exception.getErrors().size());
        assertEquals("totalQualifiedTeams", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenFourTeamsQualifiedShouldCreateTwoRounds() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildGenerateRequestWithFourTeams();
        final var groupA = buildGroup(tournamentId)
                .withName("A");
        final var groupB = buildGroup(tournamentId)
                .withName("B");
        final var groupC = buildGroup(tournamentId)
                .withName("C");
        final var groupD = buildGroup(tournamentId)
                .withName("D");

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB, groupC, groupD));
        when(groupRankingService.computeAllGroupRankings(tournamentUuid, List.of())).thenReturn(
                List.of(buildRanking(groupA.getId(), "A", 1),
                        buildRanking(groupB.getId(), "B", 1),
                        buildRanking(groupC.getId(), "C", 1),
                        buildRanking(groupD.getId(), "D", 1)));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(groupRankingService).computeAllGroupRankings(tournamentUuid, List.of());
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
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildGenerateRequestWithExtras();
        final var groupA = buildGroup(tournamentId)
                .withName("A");
        final var groupB = buildGroup(tournamentId)
                .withName("B");
        final var groupC = buildGroup(tournamentId)
                .withName("C");

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(groupA, groupB, groupC));
        when(groupRankingService.computeAllGroupRankings(tournamentUuid, List.of())).thenReturn(
                List.of(buildRankingWithExtras(groupA.getId(), "A"),
                        buildRankingWithExtras(groupB.getId(), "B"),
                        buildRankingWithExtras(groupC.getId(), "C")));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(groupRankingService).computeAllGroupRankings(tournamentUuid, List.of());
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
        final var match = buildMatch()
                .withNextMatchId(null)
                .withLoserNextMatchId(null);
        final var matchId = match.getId();
        final var matchUuid = matchId.value();
        final var request = buildMatchResultRequest();

        when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchUuid, request);

        verify(bracketMatchStore).findById(matchId);
        verify(bracketMatchStore).save(any(BracketMatch.class));

        assertNotNull(matchUpdated.getResult());
        assertEquals(3, matchUpdated.getResult().getScore1());
        assertEquals(1, matchUpdated.getResult().getScore2());
    }

    @Test
    void enterResultWhenHasNextMatchShouldAdvanceWinnerToNextMatch() {
        final var nextMatch = buildMatch()
                .withTeam1(null)
                .withTeam2(null);
        final var nextMatchId = nextMatch.getId();
        final var match = buildMatch()
                .withNextMatchId(nextMatchId)
                .withNextMatchTeamSlot(1)
                .withLoserNextMatchId(null);
        final var matchId = match.getId();
        final var request = buildMatchResultRequest();

        when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
        when(bracketMatchStore.findById(nextMatchId)).thenReturn(Optional.of(nextMatch));
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId.value(), request);

        // score1(3) > score2(1) → team1 wins, slot=1 → lands in team1 of next match
        final var winner = match.getTeam1();
        verify(bracketMatchStore, times(2)).save(any(BracketMatch.class));
        verify(bracketMatchStore).save(argThat(m -> nextMatchId.equals(m.getId()) && winner.equals(m.getTeam1())));

        assertNotNull(matchUpdated.getResult());
    }

    @Test
    void enterResultWhenDemiFinaleLoserShouldAdvanceToTroisiemePlace() {
        final var nextMatch = buildMatch()
                .withTeam1(null)
                .withTeam2(null);
        final var nextMatchId = nextMatch.getId();
        final var loserMatch = buildMatch()
                .withTeam1(null)
                .withTeam2(null);
        final var loserMatchId = loserMatch.getId();
        final var match = buildMatch()
                .withNextMatchId(nextMatchId)
                .withNextMatchTeamSlot(2)
                .withLoserNextMatchId(loserMatchId)
                .withLoserNextMatchTeamSlot(2);
        final var matchId = match.getId();

        final var request = buildMatchResultRequest();

        when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
        when(bracketMatchStore.findById(nextMatchId)).thenReturn(Optional.of(nextMatch));
        when(bracketMatchStore.findById(loserMatchId)).thenReturn(Optional.of(loserMatch));
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId.value(), request);

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
        final var match = buildMatch()
                .withNextMatchId(null)
                .withLoserNextMatchId(null)
                .withResult(buildMatchResult());
        final var matchId = match.getId();
        final var request = buildMatchResultRequest();

        when(bracketMatchStore.findById(matchId)).thenReturn(Optional.of(match));
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var matchUpdated = bracketService.enterResult(matchId.value(), request);

        verify(bracketMatchStore).findById(matchId);
        verify(bracketMatchStore).save(any(BracketMatch.class));

        assertEquals(3, matchUpdated.getResult().getScore1());
        assertEquals(1, matchUpdated.getResult().getScore2());
    }

    @Test
    void enterResultWhenDrawShouldThrowValidationException() {
        final var matchId = buildMatch().getId().value();
        final var request = buildTiedBracketMatchResultRequest();

        final var exception = assertThrows(
                ValidationException.class,
                () -> bracketService.enterResult(matchId, request));

        assertEquals(1, exception.getErrors().size());
        assertEquals("scores", exception.getErrors().getFirst().field());
    }

    @Test
    void enterResultWhenScoreNegativeShouldThrowValidationException() {
        final var matchId = buildMatch().getId().value();
        final var request = BracketMatchResultRequest.builder()
                .score1(-1)
                .score2(2)
                .build();

        final var exception = assertThrows(
                ValidationException.class,
                () -> bracketService.enterResult(matchId, request));

        assertEquals(1, exception.getErrors().size());
    }

    @Test
    void enterResultWhenMatchNotFoundShouldThrowNotFoundException() {
        final var matchId = buildMatch().getId();
        final var matchUuid = matchId.value();
        final var request = buildMatchResultRequest();

        when(bracketMatchStore.findById(matchId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> bracketService.enterResult(matchUuid, request));

        verify(bracketMatchStore).findById(matchId);

        assertTrue(exception.getMessage().contains("Match"));
    }
}
