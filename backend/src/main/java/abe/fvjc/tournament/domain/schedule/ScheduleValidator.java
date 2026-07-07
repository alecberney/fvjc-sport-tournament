package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.common.problem.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class ScheduleValidator {

    public static void validateScheduleGenerateRequest(final ScheduleGenerateRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getStartTime() == null) {
            errors.add(new ValidationException.FieldError("startTime", "L'heure de début est obligatoire"));
        }

        if (request.getMatchDurationMinutes() == null) {
            errors.add(new ValidationException.FieldError("matchDurationMinutes", "La durée d'un match est obligatoire"));
        } else if (request.getMatchDurationMinutes() < 1) {
            errors.add(new ValidationException.FieldError("matchDurationMinutes", "La durée d'un match doit être d'au moins 1 minute"));
        }

        if (request.getBreakDurationMinutes() == null) {
            errors.add(new ValidationException.FieldError("breakDurationMinutes", "La durée de pause est obligatoire"));
        } else if (request.getBreakDurationMinutes() < 0) {
            errors.add(new ValidationException.FieldError("breakDurationMinutes", "La durée de pause ne peut pas être négative"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
