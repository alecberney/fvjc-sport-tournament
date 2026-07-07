package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.schedule.RoundOverview;
import abe.fvjc.tournament.domain.schedule.ScheduleGenerateRequest;
import abe.fvjc.tournament.domain.schedule.ScheduleOverview;
import lombok.experimental.UtilityClass;

import java.util.List;

import static abe.fvjc.tournament.api.schedule.MatchApiMapper.toMatchDtos;
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

    private static List<RoundDto> toRoundDtos(final List<RoundOverview> overviews) {
        return emptyIfNull(overviews).stream()
                .map(ScheduleApiMapper::toRoundDto)
                .toList();
    }

    private static RoundDto toRoundDto(final RoundOverview overview) {
        return RoundDto.builder()
                .id(overview.getId().value())
                .number(overview.getNumber())
                .startTime(overview.getStartTime())
                .matches(toMatchDtos(overview.getMatches()))
                .build();
    }
}
