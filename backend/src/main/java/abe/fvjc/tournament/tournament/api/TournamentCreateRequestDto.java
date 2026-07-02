package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Sport;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;

@Value
@Builder
@Jacksonized
public class TournamentCreateRequestDto {

    @NotBlank
    String name;

    @NotNull
    Sport sport;

    @NotNull
    @Min(1)
    @Max(500)
    Integer numberOfFields;

    @NotNull
    @Min(1)
    Integer minPlayersPerTeam;

    @NotNull
    Integer maxPlayersPerTeam;

    @NotNull
    LocalDate date;
}
