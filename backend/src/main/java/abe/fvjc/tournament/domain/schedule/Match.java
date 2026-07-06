package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.team.domain.TeamId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Match {
    MatchId id;
    RoundId roundId;
    int field;
    GroupId groupId;
    TeamId team1Id;
    TeamId team2Id;
    MatchResult result;
}
