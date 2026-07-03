package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class MatchResultDto {
    int score1;
    int score2;
}
