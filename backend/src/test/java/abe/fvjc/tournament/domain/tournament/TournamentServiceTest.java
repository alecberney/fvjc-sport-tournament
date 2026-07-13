package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.bracket.BracketService;
import abe.fvjc.tournament.domain.group.GroupService;
import abe.fvjc.tournament.domain.schedule.RoundStore;
import abe.fvjc.tournament.domain.schedule.ScheduleService;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.team.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildCreateRequest;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Mock
    private TournamentSearchService tournamentSearchService;

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
    void findByIdWhenExistsShouldReturnTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId();

        when(tournamentSearchService.findById(id)).thenReturn(tournament);

        final var tournamentFound = tournamentService.findById(id);

        verify(tournamentSearchService).findById(id);

        assertEquals(tournament, tournamentFound);
    }

    @Test
    void deleteWhenExistsShouldCascadeAndDeleteTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId();

        when(tournamentSearchService.findById(id)).thenReturn(tournament);

        tournamentService.delete(id);

        verify(bracketService).deleteAllByTournamentId(id);
        verify(scheduleService).deleteAllByTournamentId(id);
        verify(groupService).deleteAllByTournamentId(id);
        verify(teamService).deleteAllByTournamentId(id);
        verify(tournamentStore).deleteById(tournament.getId());
    }

    @Test
    void startWhenAlreadyInProgressShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId()));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Le tournoi est déjà démarré", exception.getMessage());
    }

    @Test
    void startWhenNoRoundsShouldThrowConflictException() {
        final var tournament = buildTournament();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(roundStore.countByTournamentId(tournament.getId())).thenReturn(0);

        final var exception = assertThrows(ConflictException.class, () -> tournamentService.start(tournament.getId()));

        verify(tournamentSearchService).findById(tournament.getId());
        verify(roundStore).countByTournamentId(tournament.getId());

        assertEquals("Impossible de démarrer le tournoi sans calendrier généré", exception.getMessage());
    }

    @Test
    void startWhenValidShouldTransitionToInProgress() {
        final var tournament = buildTournament();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(roundStore.countByTournamentId(tournament.getId())).thenReturn(3);
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentStarted = tournamentService.start(tournament.getId());

        verify(tournamentSearchService).findById(tournament.getId());
        verify(roundStore).countByTournamentId(tournament.getId());
        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(IN_PROGRESS, tournamentStarted.getStatus());
    }
}
