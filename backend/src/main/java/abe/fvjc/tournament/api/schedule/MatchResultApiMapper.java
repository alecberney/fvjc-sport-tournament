package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.schedule.MatchOverview;
import abe.fvjc.tournament.domain.schedule.SubmitMatchResultRequest;
import lombok.experimental.UtilityClass;

import static abe.fvjc.tournament.api.group.GroupRankingApiMapper.toGroupRankingDto;
import static abe.fvjc.tournament.api.schedule.ScheduleApiMapper.toMatchDto;

@UtilityClass
public class MatchResultApiMapper {

    static SubmitMatchResultRequest toSubmitMatchResultRequest(final SubmitMatchResultRequestDto dto) {
        return SubmitMatchResultRequest.builder()
                .score1(dto.getScore1())
                .score2(dto.getScore2())
                .build();
    }

    static MatchResultResponseDto toMatchResultResponseDto(final MatchOverview overview,
                                                            final GroupRanking ranking) {
        return MatchResultResponseDto.builder()
                .match(toMatchDto(overview))
                .ranking(toGroupRankingDto(ranking))
                .build();
    }
}
