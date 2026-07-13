package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.schedule.Match;
import abe.fvjc.tournament.domain.schedule.Round;
import abe.fvjc.tournament.domain.schedule.RoundId;
import abe.fvjc.tournament.domain.schedule.ScheduleGenerateRequest;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomMatchId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomRoundId;

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
                .id(randomRoundId())
                .tournamentId(tournamentId)
                .number(1)
                .startTime(LocalDateTime.of(2027, 8, 15, 9, 0))
                .build();
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
}
