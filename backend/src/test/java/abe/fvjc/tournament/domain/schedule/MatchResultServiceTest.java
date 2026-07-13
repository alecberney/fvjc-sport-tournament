package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.fakes.GroupFakes;
import abe.fvjc.tournament.domain.fakes.TeamFakes;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchResultServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private MatchSearchService matchSearchService;

    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private MatchResultService matchResultService;

    @Test
    void submitResultWhenTournamentNotInProgressShouldThrowConflictException() {
        final var tournament = buildTournament(); // DRAFT
        final var matchId = MatchId.of(UUID.randomUUID());
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
                () -> matchResultService.submitResult(tournament.getId(), matchId, request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les résultats ne peuvent être saisis que pour un tournoi en cours",
                exception.getMessage());
    }

    @Test
    void submitResultWhenValidShouldSaveAndReturnMatchOverview() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId).withName("A");
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var round = buildRound(tournament.getId());
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId());
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(matchSearchService.findById(match.getId())).thenReturn(match);
        when(matchStore.save(any(Match.class))).then(returnsFirstArg());
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(t1, t2));

        final var matchOverviewFound = matchResultService.submitResult(tournament.getId(), match.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(matchSearchService).findById(match.getId());
        verify(matchStore).save(any(Match.class));
        verify(groupStore).findAllByTournamentId(tournament.getId());
        verify(teamStore).findAllByGroupId(groupId);

        assertEquals(match.getId().value(), matchOverviewFound.getId().value());
        assertNotNull(matchOverviewFound.getResult());
        assertEquals(3, matchOverviewFound.getResult().getScore1());
        assertEquals(1, matchOverviewFound.getResult().getScore2());
        assertEquals("A", matchOverviewFound.getGroupName());
        assertEquals(t1.getName(), matchOverviewFound.getTeam1().getName());
        assertEquals(t2.getName(), matchOverviewFound.getTeam2().getName());
    }
}
