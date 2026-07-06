package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

@UtilityClass
public class BracketValidator {

    public static void validateBracketGenerateRequest(final BracketGenerateRequest request, final int numGroups) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        final var totalTeams = request.getTotalQualifiedTeams();
        if (totalTeams < 2) {
            errors.add(new ValidationException.FieldError("totalQualifiedTeams",
                    "Le nombre total d'équipes qualifiées doit être d'au moins 2"));
        } else if ((totalTeams & (totalTeams - 1)) != 0) {
            errors.add(new ValidationException.FieldError("totalQualifiedTeams",
                    "Le nombre total d'équipes qualifiées doit être une puissance de 2 (2, 4, 8, 16, ...)"));
        }

        if (request.getStartTime() == null) {
            errors.add(new ValidationException.FieldError("startTime", "L'heure de début est obligatoire"));
        } else {
            try {
                LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e) {
                errors.add(new ValidationException.FieldError("startTime", "Format d'heure invalide (HH:mm attendu)"));
            }
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

    public static void validateBracketMatchResult(final BracketMatchResultRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getScore1() == null || request.getScore2() == null) {
            errors.add(new ValidationException.FieldError("scores", "Les deux scores sont obligatoires"));
        } else {
            if (request.getScore1() < 0 || request.getScore2() < 0) {
                errors.add(new ValidationException.FieldError("scores", "Le score ne peut pas être négatif"));
            }
            if (request.getScore1() > 500 || request.getScore2() > 500) {
                errors.add(new ValidationException.FieldError("scores", "Le score ne peut pas dépasser 500"));
            }
            if (request.getScore1().equals(request.getScore2())) {
                errors.add(new ValidationException.FieldError("scores",
                        "Un match éliminatoire ne peut pas se terminer sur un match nul"));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
