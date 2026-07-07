package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.schedule.RoundStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildCreateRequest;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createWhenValidRequestShouldSetDraftStatus() {
        final var request = buildCreateRequest();

        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentCreated = tournamentService.create(request);

        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(TournamentStatus.DRAFT, tournamentCreated.getStatus());
        assertNotNull(tournamentCreated.getId().value());
    }

    @Test
    void startWhenAlreadyInProgressShouldThrowConflictException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentUuid = tournament.getId().value();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> tournamentService.start(tournamentUuid));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertTrue(exception.getMessage().contains("Le tournoi est déjà démarré"));
    }

    @Test
    void startWhenNoRoundsShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(roundStore.countByTournamentId(tournamentId)).thenReturn(0);

        final var exception = assertThrows(
                ConflictException.class,
                () -> tournamentService.start(tournamentUuid));

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(roundStore).countByTournamentId(tournamentId);

        assertTrue(exception.getMessage().contains("Impossible de démarrer le tournoi sans calendrier généré"));
    }

    @Test
    void startWhenValidShouldTransitionToInProgress() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(roundStore.countByTournamentId(tournamentId)).thenReturn(3);
        when(tournamentStore.save(any(Tournament.class))).then(returnsFirstArg());

        final var tournamentStarted = tournamentService.start(tournamentUuid);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(roundStore).countByTournamentId(tournamentId);
        verify(tournamentStore).save(any(Tournament.class));

        assertEquals(TournamentStatus.IN_PROGRESS, tournamentStarted.getStatus());
    }
}
