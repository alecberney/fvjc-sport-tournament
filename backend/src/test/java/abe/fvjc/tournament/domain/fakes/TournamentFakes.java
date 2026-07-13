package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.tournament.Tournament;
import abe.fvjc.tournament.domain.tournament.TournamentCreateRequest;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTournamentId;
import static abe.fvjc.tournament.domain.tournament.Sport.PETANQUE;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.DRAFT;

@UtilityClass
public class TournamentFakes {

    public static TournamentCreateRequest buildCreateRequest() {
        return TournamentCreateRequest.builder()
            .name("Tournoi FVJC 2025")
            .sport(PETANQUE)
            .numberOfFields(4)
            .minPlayersPerTeam(2)
            .maxPlayersPerTeam(4)
            .date(LocalDate.of(2027, 8, 15))
            .build();
    }

    public static Tournament buildTournament() {
        return Tournament.builder()
            .id(randomTournamentId())
            .name("Tournoi FVJC 2025")
            .sport(PETANQUE)
            .numberOfFields(4)
            .minPlayersPerTeam(2)
            .maxPlayersPerTeam(4)
            .date(LocalDate.of(2027, 8, 15))
            .status(DRAFT)
            .build();
    }
}
