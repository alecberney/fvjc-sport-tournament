package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentSearchServiceTest {
    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private TournamentSearchService tournamentSearchService;

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var tournamentId = buildTournament().getId();
        final var tournamentUuid = tournamentId.value();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> tournamentSearchService.findById(tournamentUuid));

        verify(tournamentStore).findById(tournamentId);

        assertTrue(exception.getMessage().contains("Tournament"));
        assertTrue(exception.getMessage().contains(tournamentId.toString()));
    }

    @Test
    void findByIdWhenExistsShouldReturnTournament() {
        final var tournament = buildTournament();
        final var tournamentId = buildTournament().getId();
        final var tournamentUuid = tournamentId.value();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));

        final var tournamentFound = tournamentSearchService.findById(tournamentUuid);

        verify(tournamentStore).findById(tournamentId);

        assertEquals(tournament, tournamentFound);
    }
}
