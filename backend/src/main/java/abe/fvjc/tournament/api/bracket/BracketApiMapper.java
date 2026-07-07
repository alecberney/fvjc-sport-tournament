package abe.fvjc.tournament.api.bracket;

import abe.fvjc.tournament.domain.bracket.BracketGenerateRequest;
import abe.fvjc.tournament.domain.bracket.BracketMatch;
import abe.fvjc.tournament.domain.bracket.BracketMatchResultRequest;
import abe.fvjc.tournament.domain.bracket.BracketRound;
import abe.fvjc.tournament.domain.schedule.MatchResult;
import lombok.experimental.UtilityClass;

import java.util.List;

import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamRefDto;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class BracketApiMapper {

    static List<BracketRoundDto> toBracketRoundDtos(final List<BracketRound> rounds) {
        return emptyIfNull(rounds).stream()
                .map(BracketApiMapper::toBracketRoundDto)
                .toList();
    }

    private static BracketRoundDto toBracketRoundDto(final BracketRound round) {
        return BracketRoundDto.builder()
                .id(round.getId().value())
                .number(round.getNumber())
                .name(round.getName())
                .startTime(round.getStartTime())
                .matches(toBracketMatchDtos(round.getMatches()))
                .build();
    }

    private static List<BracketMatchDto> toBracketMatchDtos(final List<BracketMatch> matches) {
        return emptyIfNull(matches).stream()
                .map(BracketApiMapper::toBracketMatchDto)
                .toList();
    }

    static BracketMatchDto toBracketMatchDto(final BracketMatch match) {
        final var team1 = toTeamRefDto(match.getTeam1());
        final var team2 = toTeamRefDto(match.getTeam2());
        final var result = toBracketMatchResult(match.getResult());
        return BracketMatchDto.builder()
                .id(match.getId().value())
                .field(match.getField())
                .team1(team1)
                .team2(team2)
                .result(result)
                .build();
    }

    private static BracketMatchResultDto toBracketMatchResult(final MatchResult result) {
        if (result == null) {
            return null;
        }
        return BracketMatchResultDto.builder()
                .score1(result.getScore1())
                .score2(result.getScore2())
                .build();
    }

    static BracketGenerateRequest toBracketGenerateRequest(final BracketGenerateRequestDto dto) {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(dto.getTotalQualifiedTeams())
                .tieBreaker(dto.getTieBreaker())
                .startTime(dto.getStartTime())
                .matchDurationMinutes(dto.getMatchDurationMinutes())
                .breakDurationMinutes(dto.getBreakDurationMinutes())
                .build();
    }

    static BracketMatchResultRequest toBracketMatchResultRequest(final BracketMatchResultRequestDto dto) {
        return BracketMatchResultRequest.builder()
                .score1(dto.getScore1())
                .score2(dto.getScore2())
                .build();
    }
}
