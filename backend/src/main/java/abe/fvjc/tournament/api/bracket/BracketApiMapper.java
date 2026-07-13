package abe.fvjc.tournament.api.bracket;

import abe.fvjc.tournament.domain.bracket.BracketGenerateRequest;
import abe.fvjc.tournament.domain.bracket.BracketMatch;
import abe.fvjc.tournament.domain.bracket.BracketMatchResultRequest;
import abe.fvjc.tournament.domain.bracket.BracketRound;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class BracketApiMapper {

    static BracketRoundDto toBracketRoundDto(final BracketRound round) {
        return BracketRoundDto.builder()
                .id(round.getId().value())
                .number(round.getNumber())
                .name(round.getName())
                .startTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(toBracketMatchDtos(round.getMatches()))
                .build();
    }

    static List<BracketRoundDto> toBracketRoundDtos(final List<BracketRound> rounds) {
        return emptyIfNull(rounds).stream()
                .map(BracketApiMapper::toBracketRoundDto)
                .toList();
    }

    static BracketMatchDto toBracketMatchDto(final BracketMatch match) {
        final var team1 = match.getTeam1() != null
                ? BracketTeamDto.builder()
                        .id(match.getTeam1().getId().value())
                        .name(match.getTeam1().getName())
                        .build()
                : null;
        final var team2 = match.getTeam2() != null
                ? BracketTeamDto.builder()
                        .id(match.getTeam2().getId().value())
                        .name(match.getTeam2().getName())
                        .build()
                : null;
        final var result = match.getResult() != null
                ? BracketMatchResultDto.builder()
                        .score1(match.getResult().getScore1())
                        .score2(match.getResult().getScore2())
                        .build()
                : null;
        return BracketMatchDto.builder()
                .id(match.getId().value())
                .field(match.getField())
                .team1(team1)
                .team2(team2)
                .result(result)
                .build();
    }

    static List<BracketMatchDto> toBracketMatchDtos(final List<BracketMatch> matches) {
        return emptyIfNull(matches).stream()
                .map(BracketApiMapper::toBracketMatchDto)
                .toList();
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
