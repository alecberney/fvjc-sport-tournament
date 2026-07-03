package abe.fvjc.tournament.team.api;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.List;

@Value
@Builder
@JsonDeserialize(builder = TeamRegisterRequestDto.TeamRegisterRequestDtoBuilder.class)
public class TeamRegisterRequestDto {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 250, message = "Le nom ne peut pas dépasser 250 caractères")
    String name;

    @NotBlank(message = "Le prénom du responsable est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String responsibleFirstName;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String responsibleLastName;

    @NotNull(message = "Le nombre d'équipes est obligatoire")
    @Min(value = 1, message = "Le nombre d'équipes doit être d'au moins 1")
    Integer count;

    @NotNull(message = "Le tableau de paiement est obligatoire")
    List<Boolean> paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamRegisterRequestDtoBuilder {}
}
