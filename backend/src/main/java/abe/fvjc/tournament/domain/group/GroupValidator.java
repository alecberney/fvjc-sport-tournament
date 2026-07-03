package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.shared.exception.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class GroupValidator {
    public static void validateGroupGenerateRequest(final GroupGenerateRequest request, final int totalTeams) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getGroupSize() == null) {
            errors.add(new ValidationException.FieldError("groupSize", "La taille du groupe est obligatoire"));
        } else if (request.getGroupSize() < 2) {
            errors.add(new ValidationException.FieldError("groupSize", "La taille du groupe doit être d'au moins 2"));
        } else if (totalTeams < request.getGroupSize()) {
            errors.add(new ValidationException.FieldError("groupSize", "Pas assez d'équipes pour former un groupe"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
