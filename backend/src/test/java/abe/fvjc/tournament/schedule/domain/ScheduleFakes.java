package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class ScheduleFakes {

    public static ScheduleGenerateRequest buildGenerateRequest() {
        return ScheduleGenerateRequest.builder()
                .startTime("09:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static Round buildRound(final TournamentId tournamentId) {
        return Round.builder()
                .id(IdGenerator.roundId())
                .tournamentId(tournamentId)
                .number(1)
                .startTime(LocalDateTime.of(2027, 8, 15, 9, 0))
                .build();
    }

    public static Match buildMatch(final RoundId roundId) {
        return Match.builder()
                .id(IdGenerator.matchId())
                .roundId(roundId)
                .field(1)
                .groupId(GroupId.of(UUID.randomUUID()))
                .team1Id(TeamId.of(UUID.randomUUID()))
                .team2Id(TeamId.of(UUID.randomUUID()))
                .result(null)
                .build();
    }
}
