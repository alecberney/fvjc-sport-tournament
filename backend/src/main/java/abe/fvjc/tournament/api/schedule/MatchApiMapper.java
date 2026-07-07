package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.schedule.MatchOverview;
import abe.fvjc.tournament.domain.schedule.SubmitMatchResultRequest;
import lombok.experimental.UtilityClass;

import java.util.List;

import static abe.fvjc.tournament.api.group.GroupApiMapper.toGroupRefDto;
import static abe.fvjc.tournament.api.group.GroupRankingApiMapper.toGroupRankingDto;
import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamRefDto;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class MatchApiMapper {

    static List<MatchDto> toMatchDtos(final List<MatchOverview> matches) {
        return emptyIfNull(matches).stream()
                .map(MatchApiMapper::toMatchDto)
                .toList();
    }

    static MatchDto toMatchDto(final MatchOverview match) {
        final var result = match.getResult() != null
                ? MatchResultDto.builder()
                  .score1(match.getResult().getScore1())
                  .score2(match.getResult().getScore2())
                  .build()
                : null;
        return MatchDto.builder()
                .id(match.getId().value())
                .field(match.getField())
                .group(toGroupRefDto(match.getGroup()))
                .team1(toTeamRefDto(match.getTeam1()))
                .team2(toTeamRefDto(match.getTeam2()))
                .result(result)
                .build();
    }

    static SubmitMatchResultRequest toSubmitMatchResultRequest(final SubmitMatchResultRequestDto requestDto) {
        return SubmitMatchResultRequest.builder()
                .score1(requestDto.getScore1())
                .score2(requestDto.getScore2())
                .build();
    }

    static MatchResultResponseDto toMatchResultResponseDto(
            final MatchOverview overview,
            final GroupRanking ranking) {
        return MatchResultResponseDto.builder()
                .match(toMatchDto(overview))
                .ranking(toGroupRankingDto(ranking))
                .build();
    }
}
