package abe.fvjc.tournament.api.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = TeamUpdateRequestDto.TeamUpdateRequestDtoBuilder.class)
public class TeamUpdateRequestDto {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 250, message = "Le nom ne peut pas dépasser 250 caractères")
    String name;

    @NotBlank(message = "Le prénom du responsable est obligatoire")
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    String responsibleFirstName;

    @NotBlank(message = "Le nom du responsable est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String responsibleLastName;

    @NotNull(message = "Le statut de paiement est obligatoire")
    Boolean paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamUpdateRequestDtoBuilder {}
}
