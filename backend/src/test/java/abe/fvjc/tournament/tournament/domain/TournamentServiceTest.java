package abe.fvjc.tournament.tournament.domain;

import abe.fvjc.tournament.bracket.domain.BracketService;
import abe.fvjc.tournament.group.domain.GroupService;
import abe.fvjc.tournament.schedule.domain.RoundStore;
import abe.fvjc.tournament.schedule.domain.ScheduleService;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static abe.fvjc.tournament.tournament.domain.IdGenerator.tournamentId;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildCreateRequest;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private RoundStore roundStore;

    @Mock
    private TournamentStore tournamentStore;

    @Mock
    private BracketService bracketService;

    @Mock
    private GroupService groupService;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createWhenValidRequestShouldSetDraftStatus() {
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentCreated = tournamentService.create(buildCreateRequest());

        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(DRAFT, tournamentCreated.getStatus());
    }

    @Test
    void createWhenValidRequestShouldGenerateId() {
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentCreated = tournamentService.create(buildCreateRequest());

        verify(tournamentStore).save(any(Tournament.class));

        assertNotNull(tournamentCreated.getId().value());
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = tournamentId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentService.findById(id));

        verify(tournamentStore).findById(id);

        assertEquals("Tournament not found with id: " + id, exception.getMessage());
    }

    @Test
    void findByIdWhenExistsShouldReturnTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.of(tournament));

        final var tournamentFound = tournamentService.findById(id);

        verify(tournamentStore).findById(id);

        assertEquals(tournament, tournamentFound);
    }

    @Test
    void deleteWhenNotFoundShouldThrowNotFoundException() {
        final var id = tournamentId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentService.delete(id));

        verify(tournamentStore).findById(id);

        assertEquals("Tournament not found with id: " + id, exception.getMessage());
    }

    @Test
    void deleteWhenExistsShouldCascadeAndDeleteTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.of(tournament));

        tournamentService.delete(id);

        verify(bracketService).deleteAllByTournamentId(id);
        verify(scheduleService).deleteAllByTournamentId(id);
        verify(groupService).deleteAllByTournamentId(id);
        verify(teamService).deleteAllByTournamentId(id);
        verify(tournamentStore).deleteById(id);
    }

    @Test
    void startWhenNotFoundShouldThrowNotFoundException() {
        final var id = tournamentId().value();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentService.start(id));

        verify(tournamentStore).findById(id);

        assertTrue(exception.getMessage().contains("Tournament"));
    }

    @Test
    void startWhenAlreadyInProgressShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId().value()));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void startWhenNoRoundsShouldThrowConflictException() {
        final var tournament = buildTournament();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(roundStore.countByTournamentId(tournament.getId().value())).thenReturn(0);

        assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId().value()));

        verify(tournamentStore).findById(tournament.getId().value());
        verify(roundStore).countByTournamentId(tournament.getId().value());
    }

    @Test
    void startWhenValidShouldTransitionToInProgress() {
        final var tournament = buildTournament();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(roundStore.countByTournamentId(tournament.getId().value())).thenReturn(3);
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentStarted = tournamentService.start(tournament.getId().value());

        verify(tournamentStore).findById(tournament.getId().value());
        verify(roundStore).countByTournamentId(tournament.getId().value());
        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(IN_PROGRESS, tournamentStarted.getStatus());
    }
}
