package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class BracketMatch {
    BracketMatchId id;
    BracketRoundId roundId;
    int field;
    TeamRef team1;
    TeamRef team2;
    MatchResult result;
    BracketMatchId nextMatchId;
    int nextMatchTeamSlot;
}
