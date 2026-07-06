package abe.fvjc.tournament.bracket.domain;

import lombok.experimental.UtilityClass;

import static abe.fvjc.tournament.bracket.domain.TieBreaker.POINTS_DIFF;

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
}
