package abe.fvjc.tournament.persistence.team;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
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
                .groupId(entity.getGroupId() != null ? GroupId.of(entity.getGroupId()) : GroupId.empty())
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
        entity.setGroupId(team.getGroupId().isEmpty() ? null : team.getGroupId().value());
        return entity;
    }
}
