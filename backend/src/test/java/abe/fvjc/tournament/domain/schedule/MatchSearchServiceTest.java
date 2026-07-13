package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildMatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchSearchServiceTest {

    @Mock
    private MatchStore matchStore;

    @InjectMocks
    private MatchSearchService matchSearchService;

    @Test
    void findByIdWhenExistsShouldReturnMatch() {
        final var match = buildMatch(RoundId.of(UUID.randomUUID()));
        final var id = match.getId().value();

        when(matchStore.findById(MatchId.of(id))).thenReturn(Optional.of(match));

        final var matchFound = matchSearchService.findById(MatchId.of(id));

        verify(matchStore).findById(MatchId.of(id));

        assertEquals(match, matchFound);
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();

        when(matchStore.findById(MatchId.of(id))).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> matchSearchService.findById(MatchId.of(id)));

        verify(matchStore).findById(MatchId.of(id));

        assertEquals("Match not found with id: " + id, exception.getMessage());
    }
}
