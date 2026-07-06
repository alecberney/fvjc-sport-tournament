package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.team.domain.TeamId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static abe.fvjc.tournament.bracket.domain.TieBreaker.POINTS_DIFF;
import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;

@UtilityClass
public class BracketFakes {

    public static BracketGenerateRequest buildGenerateRequest() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(8)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestNotPowerOfTwo() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(6)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithFourTeams() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithExtras() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketMatch buildMatch() {
        return BracketMatch.builder()
                .id(IdGenerator.matchId())
                .roundId(IdGenerator.roundId())
                .field(1)
                .team1(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 1"))
                .team2(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 2"))
                .result(null)
                .nextMatchId(IdGenerator.matchId())
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
}
