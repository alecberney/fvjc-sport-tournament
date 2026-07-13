package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.common.problem.ValidationException;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class TeamValidator {

    public static void validateTeamRegisterRequest(final TeamRegisterRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getPaid() != null && request.getPaid().size() != request.getCount()) {
            errors.add(new ValidationException.FieldError("paid",
                "Le nombre de flags de paiement doit correspondre au nombre d'équipes"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
