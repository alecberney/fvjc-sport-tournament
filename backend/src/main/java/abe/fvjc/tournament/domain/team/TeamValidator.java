package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;

@UtilityClass
public class TeamValidator {

    public static void validateCurrentStatusIsDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                    "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation");
        }
    }

    public static void validateTeamRegisterRequest(final TeamRegisterRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getPaid() != null && request.getPaid().size() != request.getCount()) {
            final var error = new ValidationException.FieldError(
                    "paid",
                    "Le nombre de flags de paiement doit correspondre au nombre d'équipes");
            errors.add(error);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
