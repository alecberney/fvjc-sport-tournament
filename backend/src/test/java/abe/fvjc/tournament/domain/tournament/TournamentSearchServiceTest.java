package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentSearchServiceTest {

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private TournamentSearchService tournamentSearchService;

    @Test
    void findByIdWhenExistsShouldReturnTournament() {
        final var tournament = buildTournament();
        final var id = tournament.getId().value();

        when(tournamentStore.findById(TournamentId.of(id))).thenReturn(Optional.of(tournament));

        final var tournamentFound = tournamentSearchService.findById(TournamentId.of(id));

        verify(tournamentStore).findById(TournamentId.of(id));

        assertEquals(tournament, tournamentFound);
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();

        when(tournamentStore.findById(TournamentId.of(id))).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> tournamentSearchService.findById(TournamentId.of(id)));

        verify(tournamentStore).findById(TournamentId.of(id));

        assertEquals("Tournament not found with id: " + id, exception.getMessage());
    }
}
