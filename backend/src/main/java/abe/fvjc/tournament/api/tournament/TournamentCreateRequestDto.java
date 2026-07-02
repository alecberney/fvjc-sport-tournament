package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.Sport;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.LocalDate;

@Value
@Builder
@JsonDeserialize(builder = TournamentCreateRequestDto.TournamentCreateRequestDtoBuilder.class)
public class TournamentCreateRequestDto {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 250, message = "Le nom ne peut pas dépasser 250 caractères")
    String name;

    @NotNull(message = "Le sport est obligatoire")
    Sport sport;

    @NotNull(message = "Le nombre de terrains est obligatoire")
    @Min(value = 1, message = "Le nombre de terrains doit être d'au moins 1")
    @Max(value = 500, message = "Le nombre de terrains ne peut pas dépasser 500")
    Integer numberOfFields;

    @NotNull(message = "Le nombre minimum de joueurs est obligatoire")
    @Min(value = 1, message = "Le nombre minimum de joueurs doit être d'au moins 1")
    Integer minPlayersPerTeam;

    @NotNull(message = "Le nombre maximum de joueurs est obligatoire")
    Integer maxPlayersPerTeam;

    @NotNull(message = "La date est obligatoire")
    LocalDate date;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TournamentCreateRequestDtoBuilder {}
}
