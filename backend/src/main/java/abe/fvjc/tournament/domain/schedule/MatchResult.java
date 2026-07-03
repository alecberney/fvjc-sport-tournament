package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchResult {
    int score1;
    int score2;
}
