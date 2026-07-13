package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.common.problem.ValidationException;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.ArrayList;

@UtilityClass
public class TournamentValidator {

    public static void validateTournamentCreateRequest(final TournamentCreateRequest request) {
        final var errors = new ArrayList<ValidationException.FieldError>();

        if (request.getMaxPlayersPerTeam() < request.getMinPlayersPerTeam()) {
            errors.add(new ValidationException.FieldError("maxPlayersPerTeam",
                "Le nombre maximum de joueurs doit être supérieur ou égal au minimum"));
        }

        if (request.getDate().isBefore(LocalDate.now())) {
            errors.add(new ValidationException.FieldError("date",
                "La date doit être aujourd'hui ou dans le futur"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
