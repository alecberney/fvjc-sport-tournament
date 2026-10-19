package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.schedule.Match;
import abe.fvjc.tournament.domain.schedule.MatchPair;
import abe.fvjc.tournament.domain.schedule.Round;
import abe.fvjc.tournament.domain.schedule.RoundId;
import abe.fvjc.tournament.domain.schedule.ScheduleGenerateRequest;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomGroupId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomMatchId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomRoundId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTeamId;

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

    public static Map<GroupId, List<TeamId>> buildTeamIdsByGroupId(final int... groupSizes) {
        final var teamIdsByGroupId = new LinkedHashMap<GroupId, List<TeamId>>();
        for (final var groupSize : groupSizes) {
            final var teamIds = new ArrayList<TeamId>();
            for (int i = 0; i < groupSize; i++) {
                teamIds.add(randomTeamId());
            }
            teamIdsByGroupId.put(randomGroupId(), teamIds);
        }
        return teamIdsByGroupId;
    }

    public static MatchPair buildMatchPair(final GroupId groupId, final TeamId team1Id, final TeamId team2Id) {
        return new MatchPair(groupId, team1Id, team2Id);
    }
}
