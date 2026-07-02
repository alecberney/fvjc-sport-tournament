package abe.fvjc.tournament.shared.exception;

import tools.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static abe.fvjc.tournament.shared.exception.ValidationErrorResponse.FieldError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        final var bindingErrors = ex.getBindingResult().getFieldErrors();
        final var errors = bindingErrors.stream()
                .map(fe -> FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(ValidationErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleDomainValidation(ValidationException ex) {
        final var errors = ex.getErrors().stream()
                .map(e -> FieldError.builder()
                        .field(e.field())
                        .message(e.message())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(ValidationErrorResponse.builder().errors(errors).build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            final var fieldName = ife.getPath().isEmpty()
                    ? "unknown"
                    : ife.getPath().getLast().getPropertyName();
            final var errors = List.of(FieldError.builder()
                    .field(fieldName)
                    .message("Le sport sélectionné n'est pas valide")
                    .build());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ValidationErrorResponse.builder().errors(errors).build());
        }
        return ResponseEntity.badRequest()
                .body(ValidationErrorResponse.builder().errors(List.of()).build());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFound(NotFoundException ex) {
        return ResponseEntity.notFound().build();
    }
}
