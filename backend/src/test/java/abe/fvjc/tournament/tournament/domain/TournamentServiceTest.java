package abe.fvjc.tournament.tournament.domain;

import abe.fvjc.tournament.shared.exception.NotFoundException;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentStore tournamentStore;

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
}
