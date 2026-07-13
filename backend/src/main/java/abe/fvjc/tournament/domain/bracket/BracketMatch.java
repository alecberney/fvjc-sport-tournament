package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.schedule.MatchResult;
import abe.fvjc.tournament.domain.team.TeamRef;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class BracketMatch {
    BracketMatchId id;
    BracketRoundId roundId;
    int field;
    TeamRef team1;
    TeamRef team2;
    MatchResult result;
    BracketMatchId nextMatchId;
    int nextMatchTeamSlot;
    BracketMatchId loserNextMatchId;
    int loserNextMatchTeamSlot;
}
