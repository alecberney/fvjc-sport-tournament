package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.schedule.MatchOverview;
import abe.fvjc.tournament.domain.schedule.RoundOverview;
import abe.fvjc.tournament.domain.schedule.ScheduleGenerateRequest;
import abe.fvjc.tournament.domain.schedule.ScheduleOverview;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamRefDto;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

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
                .rounds(toRoundDtos(overview.getRounds()))
                .build();
    }

    private static RoundDto toRoundDto(final RoundOverview overview) {
        return RoundDto.builder()
                .id(overview.getId().value())
                .number(overview.getNumber())
                .startTime(overview.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(toMatchDtos(overview.getMatches()))
                .build();
    }

    private static List<RoundDto> toRoundDtos(final List<RoundOverview> overviews) {
        return emptyIfNull(overviews).stream()
                .map(ScheduleApiMapper::toRoundDto)
                .toList();
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
                .team1(toTeamRefDto(overview.getTeam1()))
                .team2(toTeamRefDto(overview.getTeam2()))
                .result(result)
                .build();
    }

    static List<MatchDto> toMatchDtos(final List<MatchOverview> overviews) {
        return emptyIfNull(overviews).stream()
                .map(ScheduleApiMapper::toMatchDto)
                .toList();
    }
}
