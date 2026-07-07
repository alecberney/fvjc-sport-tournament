package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.common.problem.ValidationException;
import org.junit.jupiter.api.Test;

import static abe.fvjc.tournament.domain.schedule.MatchResultValidator.validateSubmitMatchResultRequest;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchResultValidatorTest {

    @Test
    void validateWhenScore1NullShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(null)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().getFirst().field());
    }

    @Test
    void validateWhenScore2NullShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(3)
                .score2(null)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score2", exception.getErrors().getFirst().field());
    }

    @Test
    void validateWhenScore1NegativeShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(-1)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().getFirst().field());
    }

    @Test
    void validateWhenScore1Above500ShouldThrowValidationException() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(501)
                .score2(3)
                .build();

        final var exception = assertThrows(ValidationException.class,
                () -> validateSubmitMatchResultRequest(request));

        assertEquals("score1", exception.getErrors().getFirst().field());
    }

    @Test
    void validateWhenValidShouldNotThrow() {
        final var request = SubmitMatchResultRequest.builder()
                .score1(3)
                .score2(1)
                .build();

        assertDoesNotThrow(() -> validateSubmitMatchResultRequest(request));
    }
}
