package abe.fvjc.tournament.domain.bracket;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalTime;

@Value
@With
@Builder
public class BracketGenerateRequest {
    int totalQualifiedTeams;
    TieBreaker tieBreaker;
    LocalTime startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
