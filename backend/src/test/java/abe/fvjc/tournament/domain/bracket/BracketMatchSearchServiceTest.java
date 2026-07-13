package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildMatch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BracketMatchSearchServiceTest {

    @Mock
    private BracketMatchStore bracketMatchStore;

    @InjectMocks
    private BracketMatchSearchService bracketMatchSearchService;

    @Test
    void findByIdWhenExistsShouldReturnMatch() {
        final var bracketMatch = buildMatch();
        final var id = bracketMatch.getId().value();

        when(bracketMatchStore.findById(BracketMatchId.of(id))).thenReturn(Optional.of(bracketMatch));

        final var bracketMatchFound = bracketMatchSearchService.findById(BracketMatchId.of(id));

        verify(bracketMatchStore).findById(BracketMatchId.of(id));

        assertEquals(bracketMatch, bracketMatchFound);
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();

        when(bracketMatchStore.findById(BracketMatchId.of(id))).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> bracketMatchSearchService.findById(BracketMatchId.of(id)));

        verify(bracketMatchStore).findById(BracketMatchId.of(id));

        assertEquals("Match not found with id: " + id, exception.getMessage());
    }
}
