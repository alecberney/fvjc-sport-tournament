package abe.fvjc.tournament.bracket.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BracketMatchResultRequest {
    Integer score1;
    Integer score2;
}
