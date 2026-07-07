package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.schedule.Round;
import abe.fvjc.tournament.domain.schedule.ScheduleGenerateRequest;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.LocalTime;

@UtilityClass
public class ScheduleFakes {

    public static ScheduleGenerateRequest buildGenerateRequest() {
        return ScheduleGenerateRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static Round buildRound(final TournamentId tournamentId) {
        return Round.builder()
                .id(IdGenerator.randomRoundId())
                .tournamentId(tournamentId)
                .number(1)
                .startTime(LocalDateTime.of(2027, 8, 15, 9, 0))
                .build();
    }
}
