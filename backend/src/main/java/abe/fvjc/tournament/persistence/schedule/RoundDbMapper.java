package abe.fvjc.tournament.persistence.schedule;

import abe.fvjc.tournament.domain.schedule.Round;
import abe.fvjc.tournament.domain.schedule.RoundId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
class RoundDbMapper {

    static Round toRound(final RoundEntity entity) {
        return Round.builder()
                .id(RoundId.of(entity.getId()))
                .tournamentId(TournamentId.of(entity.getTournamentId()))
                .number(entity.getNumber())
                .startTime(LocalDateTime.parse(entity.getStartTime(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    static RoundEntity toRoundEntity(final Round round) {
        final var entity = new RoundEntity();
        entity.setId(round.getId().value());
        entity.setTournamentId(round.getTournamentId().value());
        entity.setNumber(round.getNumber());
        entity.setStartTime(round.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return entity;
    }
}
