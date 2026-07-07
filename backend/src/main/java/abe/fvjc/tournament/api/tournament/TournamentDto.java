package abe.fvjc.tournament.api.tournament;

import abe.fvjc.tournament.domain.tournament.Sport;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class TournamentDto {
    UUID id;
    String name;
    Sport sport;
    int numberOfFields;
    int minPlayersPerTeam;
    int maxPlayersPerTeam;
    LocalDate date;
    TournamentStatus status;
}
