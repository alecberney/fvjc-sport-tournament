package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketMatchId;
import abe.fvjc.tournament.bracket.domain.BracketRound;
import abe.fvjc.tournament.bracket.domain.BracketRoundId;
import abe.fvjc.tournament.schedule.domain.MatchResult;
import abe.fvjc.tournament.schedule.domain.TeamRef;
import abe.fvjc.tournament.team.domain.TeamId;

import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
class BracketDbMapper {

    static BracketRound toBracketRound(final BracketRoundEntity entity) {
        return BracketRound.builder()
                .id(BracketRoundId.of(entity.getId()))
                .tournamentId(TournamentId.of(entity.getTournamentId()))
                .number(entity.getNumber())
                .name(entity.getName())
                .startTime(LocalDateTime.parse(entity.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .matches(List.of())
                .build();
    }

    static BracketRoundEntity toBracketRoundEntity(final BracketRound round) {
        final var entity = new BracketRoundEntity();
        entity.setId(round.getId().value());
        entity.setTournamentId(round.getTournamentId().value());
        entity.setNumber(round.getNumber());
        entity.setName(round.getName());
        entity.setStartTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }

    static BracketMatch toBracketMatch(final BracketMatchEntity entity) {
        final var team1 = entity.getTeam1Id() != null
                ? toTeamRef(TeamId.of(entity.getTeam1Id()), entity.getTeam1Name())
                : null;
        final var team2 = entity.getTeam2Id() != null
                ? toTeamRef(TeamId.of(entity.getTeam2Id()), entity.getTeam2Name())
                : null;
        final var result = entity.getScore1() != null
                ? MatchResult.builder().score1(entity.getScore1()).score2(entity.getScore2()).build()
                : null;
        final var nextMatchId = entity.getNextMatchId() != null
                ? BracketMatchId.of(entity.getNextMatchId())
                : null;
        return BracketMatch.builder()
                .id(BracketMatchId.of(entity.getId()))
                .roundId(BracketRoundId.of(entity.getRoundId()))
                .field(entity.getField())
                .team1(team1)
                .team2(team2)
                .result(result)
                .nextMatchId(nextMatchId)
                .nextMatchTeamSlot(entity.getNextMatchTeamSlot())
                .build();
    }

    static BracketMatchEntity toBracketMatchEntity(final BracketMatch match) {
        final var entity = new BracketMatchEntity();
        entity.setId(match.getId().value());
        entity.setRoundId(match.getRoundId().value());
        entity.setField(match.getField());
        if (match.getTeam1() != null) {
            entity.setTeam1Id(match.getTeam1().getId().value());
            entity.setTeam1Name(match.getTeam1().getName());
        }
        if (match.getTeam2() != null) {
            entity.setTeam2Id(match.getTeam2().getId().value());
            entity.setTeam2Name(match.getTeam2().getName());
        }
        if (match.getResult() != null) {
            entity.setScore1(match.getResult().getScore1());
            entity.setScore2(match.getResult().getScore2());
        }
        entity.setNextMatchId(match.getNextMatchId() != null ? match.getNextMatchId().value() : null);
        entity.setNextMatchTeamSlot(match.getNextMatchTeamSlot());
        return entity;
    }
}
