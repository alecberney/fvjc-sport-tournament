package abe.fvjc.tournament.domain.bracket;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class BracketGenerateRequest {
    int totalQualifiedTeams;
    TieBreaker tieBreaker;
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
