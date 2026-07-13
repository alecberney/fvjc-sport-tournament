package abe.fvjc.tournament.api.bracket;

import abe.fvjc.tournament.domain.bracket.TieBreaker;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketGenerateRequestDto {
    @Min(2)
    int totalQualifiedTeams;

    @NotNull
    TieBreaker tieBreaker;

    @NotNull
    String startTime;

    @NotNull
    @Min(1)
    Integer matchDurationMinutes;

    @NotNull
    @Min(0)
    Integer breakDurationMinutes;
}
