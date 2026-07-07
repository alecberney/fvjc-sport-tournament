package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.schedule.Match;
import abe.fvjc.tournament.domain.schedule.MatchResult;
import abe.fvjc.tournament.domain.schedule.RoundId;
import abe.fvjc.tournament.domain.schedule.SubmitMatchResultRequest;
import abe.fvjc.tournament.domain.team.TeamId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomMatchId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomRoundId;

@UtilityClass
public class MatchFakes {
    public static Match buildMatch() {
        return buildMatch(randomRoundId());
    }

    public static Match buildMatch(final RoundId roundId) {
        return Match.builder()
                .id(randomMatchId())
                .roundId(roundId)
                .field(1)
                .groupId(GroupId.of(UUID.randomUUID()))
                .team1Id(TeamId.of(UUID.randomUUID()))
                .team2Id(TeamId.of(UUID.randomUUID()))
                .result(null)
                .build();
    }

    public static SubmitMatchResultRequest buildSubmitMatchResultRequest() {
        return SubmitMatchResultRequest.builder()
                .score1(3)
                .score2(1)
                .build();
    }

    public static MatchResult buildMatchResult() {
        return MatchResult.builder()
                .score1(1)
                .score2(3)
                .build();
    }
}
