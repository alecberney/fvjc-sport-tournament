package abe.fvjc.tournament.domain.common.problem;

public class ConflictException extends RuntimeException {
    public ConflictException(final String message) {
        super(message);
    }
}
