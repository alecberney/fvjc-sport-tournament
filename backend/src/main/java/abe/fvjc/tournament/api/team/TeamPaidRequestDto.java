package abe.fvjc.tournament.api.team;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;

@Value
@Builder
@JsonDeserialize(builder = TeamPaidRequestDto.TeamPaidRequestDtoBuilder.class)
public class TeamPaidRequestDto {

    @NotNull(message = "Le statut de paiement est obligatoire")
    Boolean paid;

    @JsonPOJOBuilder(withPrefix = "")
    public static class TeamPaidRequestDtoBuilder {}
}
