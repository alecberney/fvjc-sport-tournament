package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupRankingEntryDto {
    int rank;
    MatchTeamDto team;
    int played;
    int wins;
    int draws;
    int defeats;
    int goalsFor;
    int goalsAgainst;
    int goalDifference;
    int points;
}
