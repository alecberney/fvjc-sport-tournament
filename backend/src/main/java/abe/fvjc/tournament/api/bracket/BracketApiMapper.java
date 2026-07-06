package abe.fvjc.tournament.bracket.api;

import abe.fvjc.tournament.bracket.domain.BracketGenerateRequest;
import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketRound;
import lombok.experimental.UtilityClass;

import java.time.format.DateTimeFormatter;

@UtilityClass
public class BracketApiMapper {

    static BracketRoundDto toBracketRoundDto(final BracketRound round) {
        return BracketRoundDto.builder()
                .id(round.getId().value())
                .number(round.getNumber())
                .name(round.getName())
                .startTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(round.getMatches().stream()
                        .map(BracketApiMapper::toBracketMatchDto)
                        .toList())
                .build();
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

    static BracketGenerateRequest toBracketGenerateRequest(final BracketGenerateRequestDto dto) {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(dto.getTotalQualifiedTeams())
                .tieBreaker(dto.getTieBreaker())
                .startTime(dto.getStartTime())
                .matchDurationMinutes(dto.getMatchDurationMinutes())
                .breakDurationMinutes(dto.getBreakDurationMinutes())
                .build();
    }
}
