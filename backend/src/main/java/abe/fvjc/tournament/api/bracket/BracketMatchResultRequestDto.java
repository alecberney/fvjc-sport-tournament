package abe.fvjc.tournament.bracket.api;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketMatchResultRequestDto {
    @NotNull Integer score1;
    @NotNull Integer score2;
}
