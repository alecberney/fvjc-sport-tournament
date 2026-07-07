package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.api.team.TeamRefDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupRankingEntryDto {
    int rank;
    TeamRefDto team;
    int played;
    int wins;
    int draws;
    int defeats;
    int goalsFor;
    int goalsAgainst;
    int goalDifference;
    int points;
}
