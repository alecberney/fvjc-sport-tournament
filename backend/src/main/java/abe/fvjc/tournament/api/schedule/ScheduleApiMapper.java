package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.MatchOverview;
import abe.fvjc.tournament.schedule.domain.RoundOverview;
import abe.fvjc.tournament.schedule.domain.ScheduleGenerateRequest;
import abe.fvjc.tournament.schedule.domain.ScheduleOverview;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class ScheduleApiMapper {

    static ScheduleGenerateRequest toScheduleGenerateRequest(final ScheduleGenerateRequestDto dto) {
        return ScheduleGenerateRequest.builder()
                .startTime(dto.getStartTime())
                .matchDurationMinutes(dto.getMatchDurationMinutes())
                .breakDurationMinutes(dto.getBreakDurationMinutes())
                .build();
    }

    static ScheduleDto toScheduleDto(final ScheduleOverview overview) {
        return ScheduleDto.builder()
                .totalRounds(overview.getTotalRounds())
                .totalMatches(overview.getTotalMatches())
                .rounds(overview.getRounds().stream()
                        .map(ScheduleApiMapper::toRoundDto)
                        .toList())
                .build();
    }

    private static RoundDto toRoundDto(final RoundOverview overview) {
        return RoundDto.builder()
                .id(overview.getId().value())
                .number(overview.getNumber())
                .startTime(overview.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(overview.getMatches().stream()
                        .map(ScheduleApiMapper::toMatchDto)
                        .toList())
                .build();
    }

    static MatchDto toMatchDto(final MatchOverview overview) {
        final var result = overview.getResult() != null
                ? MatchResultDto.builder()
                        .score1(overview.getResult().getScore1())
                        .score2(overview.getResult().getScore2())
                        .build()
                : null;
        return MatchDto.builder()
                .id(overview.getId().value())
                .field(overview.getField())
                .groupId(overview.getGroupId().value())
                .groupName(overview.getGroupName())
                .team1(toMatchTeamDto(overview.getTeam1()))
                .team2(toMatchTeamDto(overview.getTeam2()))
                .result(result)
                .build();
    }

    private static MatchTeamDto toMatchTeamDto(final TeamRef ref) {
        return MatchTeamDto.builder()
                .id(ref.getId().value())
                .name(ref.getName())
                .build();
    }
}
