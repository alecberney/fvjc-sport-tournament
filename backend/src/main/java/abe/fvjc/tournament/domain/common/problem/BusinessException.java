package abe.fvjc.tournament.domain.common.problem;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
