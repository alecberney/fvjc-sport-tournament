package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.bracket.BracketGenerateRequest;
import abe.fvjc.tournament.domain.bracket.BracketMatch;
import abe.fvjc.tournament.domain.bracket.BracketMatchResultRequest;
import abe.fvjc.tournament.domain.team.TeamId;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;
import java.util.UUID;

import static abe.fvjc.tournament.domain.bracket.TieBreaker.POINTS_DIFF;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomBracketMatchId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomBracketRoundId;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;

@UtilityClass
public class BracketFakes {

    public static BracketGenerateRequest buildGenerateRequest() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(8)
                .tieBreaker(POINTS_DIFF)
                .startTime(LocalTime.of(14, 0))
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestNotPowerOfTwo() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(6)
                .tieBreaker(POINTS_DIFF)
                .startTime(LocalTime.of(14, 0))
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithFourTeams() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime(LocalTime.of(14, 0))
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithExtras() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime(LocalTime.of(14, 0))
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketMatch buildMatch() {
        return BracketMatch.builder()
                .id(randomBracketMatchId())
                .roundId(randomBracketRoundId())
                .field(1)
                .team1(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 1"))
                .team2(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 2"))
                .result(null)
                .nextMatchId(randomBracketMatchId())
                .nextMatchTeamSlot(1)
                .loserNextMatchId(null)
                .loserNextMatchTeamSlot(0)
                .build();
    }

    public static BracketMatchResultRequest buildMatchResultRequest() {
        return BracketMatchResultRequest.builder()
                .score1(3)
                .score2(1)
                .build();
    }

    public static BracketMatchResultRequest buildTiedBracketMatchResultRequest() {
        return BracketMatchResultRequest.builder()
                .score1(2)
                .score2(2)
                .build();
    }
}
