package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.team.TeamRef;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupRankingEntry {
    int rank;
    TeamRef team;
    int played;
    int wins;
    int draws;
    int defeats;
    int goalsFor;
    int goalsAgainst;
    int goalDifference;
    int points;
}
