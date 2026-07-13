package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.common.problem.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class ResultValidator {

    public static void validateSubmitMatchResultRequest(final SubmitMatchResultRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();
        validateScore(request.getScore1(), "score1", errors);
        validateScore(request.getScore2(), "score2", errors);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private static void validateScore(final Integer score, final String field,
                                       final java.util.List<ValidationException.FieldError> errors) {
        if (score == null) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score de l'équipe est obligatoire"));
        } else if (score < 0) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score ne peut pas être négatif"));
        } else if (score > 500) {
            errors.add(new ValidationException.FieldError(field,
                    "Le score ne peut pas dépasser 500"));
        }
    }
}
