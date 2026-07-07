package abe.fvjc.tournament.api.team;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class TeamPaymentRequestDto {
    @NotNull
    Boolean paid;
}
