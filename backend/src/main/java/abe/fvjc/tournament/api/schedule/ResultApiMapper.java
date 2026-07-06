package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.MatchOverview;
import abe.fvjc.tournament.schedule.domain.SubmitMatchResultRequest;
import lombok.experimental.UtilityClass;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;
import static abe.fvjc.tournament.schedule.api.ScheduleApiMapper.toMatchDto;

@UtilityClass
public class ResultApiMapper {

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
