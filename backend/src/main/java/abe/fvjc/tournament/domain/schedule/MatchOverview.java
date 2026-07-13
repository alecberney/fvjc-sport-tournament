package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.team.TeamRef;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchOverview {
    MatchId id;
    int field;
    GroupId groupId;
    String groupName;
    TeamRef team1;
    TeamRef team2;
    MatchResult result;
}
