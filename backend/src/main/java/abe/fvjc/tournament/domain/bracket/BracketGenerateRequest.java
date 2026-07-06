package abe.fvjc.tournament.bracket.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class BracketGenerateRequest {
    int totalQualifiedTeams;
    TieBreaker tieBreaker;
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
