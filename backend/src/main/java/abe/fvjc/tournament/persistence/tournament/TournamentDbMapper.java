package abe.fvjc.tournament.tournament.persistence;

import abe.fvjc.tournament.tournament.domain.Sport;
import abe.fvjc.tournament.tournament.domain.Tournament;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.util.UUID;

@UtilityClass
class TournamentDbMapper {

    static Tournament toTournament(TournamentEntity entity) {
        return Tournament.builder()
            .id(TournamentId.of(entity.getId()))
            .name(entity.getName())
            .sport(Sport.valueOf(entity.getSport()))
            .numberOfFields(entity.getNumberOfFields())
            .minPlayersPerTeam(entity.getMinPlayersPerTeam())
            .maxPlayersPerTeam(entity.getMaxPlayersPerTeam())
            .date(LocalDate.parse(entity.getDate()))
            .status(TournamentStatus.valueOf(entity.getStatus()))
            .build();
    }

    static TournamentEntity toTournamentEntity(Tournament tournament) {
        final var entity = new TournamentEntity();
        final var id = tournament.getId().isEmpty()
                ? UUID.randomUUID()
                : tournament.getId().value();
        entity.setId(id);
        entity.setName(tournament.getName());
        entity.setSport(tournament.getSport().name());
        entity.setNumberOfFields(tournament.getNumberOfFields());
        entity.setMinPlayersPerTeam(tournament.getMinPlayersPerTeam());
        entity.setMaxPlayersPerTeam(tournament.getMaxPlayersPerTeam());
        entity.setDate(tournament.getDate().toString());
        entity.setStatus(tournament.getStatus().name());
        return entity;
    }
}
