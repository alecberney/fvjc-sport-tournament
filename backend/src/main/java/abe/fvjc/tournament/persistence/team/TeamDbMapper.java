package abe.fvjc.tournament.team.persistence;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class TeamDbMapper {

    static Team toTeam(final TeamEntity entity) {
        return Team.builder()
            .id(TeamId.of(entity.getId()))
            .name(entity.getName())
            .paid(entity.isPaid())
            .organisationId(OrganisationId.of(entity.getOrganisationId()))
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static TeamEntity toTeamEntity(final Team team) {
        final var entity = new TeamEntity();
        final var id = team.getId().isEmpty()
                ? UUID.randomUUID()
                : team.getId().value();
        entity.setId(id);
        entity.setName(team.getName());
        entity.setPaid(team.isPaid());
        entity.setOrganisationId(team.getOrganisationId().value());
        entity.setTournamentId(team.getTournamentId().value());
        return entity;
    }
}
