package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
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
