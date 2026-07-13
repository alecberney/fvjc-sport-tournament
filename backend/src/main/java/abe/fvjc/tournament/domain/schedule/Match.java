package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.team.TeamId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class Match {
    MatchId id;
    RoundId roundId;
    int field;
    GroupId groupId;
    TeamId team1Id;
    TeamId team2Id;
    MatchResult result;
}
