package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.MatchFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.MatchFakes.buildSubmitMatchResultRequest;
import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {
    @Mock
    private GroupStore groupStore;
    @Mock
    private MatchStore matchStore;
    @Mock
    private TeamStore teamStore;
    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private MatchService matchService;

    @Test
    void submitResultWhenTournamentNotInProgressShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var tournamentUuid = tournament.getId().value();
        final var matchId = buildMatch().getId();
        final var request = buildSubmitMatchResultRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> matchService.submitResult(tournamentUuid, matchId.value(), request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("Les résultats ne peuvent être saisis que pour un tournoi en cours", exception.getMessage());
    }



    @Test
    void submitResultWhenMatchNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentUuid = tournament.getId().value();
        final var matchId = buildMatch().getId();
        final var request = buildSubmitMatchResultRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(matchStore.findById(matchId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> matchService.submitResult(tournamentUuid, matchId.value(), request));

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(matchStore).findById(matchId);

        assertTrue(exception.getMessage().contains("Match"));
    }

    @Test
    void submitResultWhenValidShouldSaveAndReturnMatchOverview() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentId = tournament.getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(tournamentId)
                .withName("A");
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, tournamentId, groupId);
        final var team2 = buildTeam(organisationId, tournamentId, groupId);
        final var round = buildRound(tournamentId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(team1.getId())
                .withTeam2Id(team2.getId());
        final var matchId = match.getId();
        final var request = buildSubmitMatchResultRequest();

        when(tournamentSearchService.findById(tournamentId.value())).thenReturn(tournament);
        when(matchStore.findById(matchId)).thenReturn(Optional.of(match));
        when(matchStore.save(any(Match.class))).then(returnsFirstArg());
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team1, team2));

        final var matchOverviewFound = matchService.submitResult(tournamentId.value(), matchId.value(), request);

        verify(tournamentSearchService).findById(tournamentId.value());
        verify(matchStore).findById(matchId);
        verify(matchStore).save(any(Match.class));
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);

        assertEquals(matchId.value(), matchOverviewFound.getId().value());
        assertNotNull(matchOverviewFound.getResult());
        assertEquals(3, matchOverviewFound.getResult().getScore1());
        assertEquals(1, matchOverviewFound.getResult().getScore2());
        assertEquals("A", matchOverviewFound.getGroup().getName());
        assertEquals(team1.getName(), matchOverviewFound.getTeam1().getName());
        assertEquals(team2.getName(), matchOverviewFound.getTeam2().getName());
    }
}
