package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.buildMatch;
import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private ResultService resultService;

    @Test
    void submitResultWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var tournamentId = UUID.randomUUID();
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
                () -> resultService.submitResult(tournamentId, matchId, request));

        verify(tournamentStore).findById(tournamentId);

        assertTrue(exception.getMessage().contains("Tournament"));
    }

    @Test
    void submitResultWhenTournamentNotInProgressShouldThrowConflictException() {
        final var tournament = buildTournament(); // DRAFT
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
                () -> resultService.submitResult(tournament.getId().value(), matchId, request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void submitResultWhenMatchNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var matchId = UUID.randomUUID();
        final var request = SubmitMatchResultRequest.builder().score1(3).score2(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(matchStore.findById(matchId)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
                () -> resultService.submitResult(tournament.getId().value(), matchId, request));

        verify(matchStore).findById(matchId);

        assertTrue(exception.getMessage().contains("Match"));
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

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(matchStore.findById(match.getId().value())).thenReturn(Optional.of(match));
        when(matchStore.save(any(Match.class))).then(returnsFirstArg());
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(t1, t2));

        final var matchOverviewFound = resultService.submitResult(tournament.getId().value(), match.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(matchStore).findById(match.getId().value());
        verify(matchStore).save(any(Match.class));
        verify(groupStore).findAllByTournamentId(tournament.getId().value());
        verify(teamStore).findAllByGroupId(groupId.value());

        assertEquals(match.getId().value(), matchOverviewFound.getId().value());
        assertNotNull(matchOverviewFound.getResult());
        assertEquals(3, matchOverviewFound.getResult().getScore1());
        assertEquals(1, matchOverviewFound.getResult().getScore2());
        assertEquals("A", matchOverviewFound.getGroupName());
        assertEquals(t1.getName(), matchOverviewFound.getTeam1().getName());
        assertEquals(t2.getName(), matchOverviewFound.getTeam2().getName());
    }
}
