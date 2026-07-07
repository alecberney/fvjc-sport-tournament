package abe.fvjc.tournament.api.bracket;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class BracketMatchResultDto {
    int score1;
    int score2;
}
