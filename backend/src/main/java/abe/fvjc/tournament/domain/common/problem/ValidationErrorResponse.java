package abe.fvjc.tournament.shared.exception;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ValidationErrorResponse {
    List<FieldError> errors;

    @Value
    @Builder
    public static class FieldError {
        String field;
        String message;
    }
}
