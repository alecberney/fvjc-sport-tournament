package abe.fvjc.tournament.tournament.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
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
