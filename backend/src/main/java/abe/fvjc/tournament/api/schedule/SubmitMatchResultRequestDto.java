package abe.fvjc.tournament.api.schedule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class SubmitMatchResultRequestDto {
    @NotNull
    @Min(0)
    @Max(500)
    Integer score1;

    @NotNull
    @Min(0)
    @Max(500)
    Integer score2;
}
