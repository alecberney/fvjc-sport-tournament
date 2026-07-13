package abe.fvjc.tournament.domain.tournament;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class TournamentCreateRequest {
    String name;
    Sport sport;
    int numberOfFields;
    int minPlayersPerTeam;
    int maxPlayersPerTeam;
    LocalDate date;
}
