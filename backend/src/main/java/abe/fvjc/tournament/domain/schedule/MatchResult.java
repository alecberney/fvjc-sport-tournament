package abe.fvjc.tournament.domain.schedule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchResult {
    int score1;
    int score2;
}
