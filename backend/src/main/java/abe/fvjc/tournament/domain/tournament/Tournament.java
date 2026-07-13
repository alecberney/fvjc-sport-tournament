package abe.fvjc.tournament.domain.tournament;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDate;

@Value
@With
@Builder
public class Tournament {
    TournamentId id;
    String name;
    Sport sport;
    int numberOfFields;
    int minPlayersPerTeam;
    int maxPlayersPerTeam;
    LocalDate date;
    TournamentStatus status;
}
