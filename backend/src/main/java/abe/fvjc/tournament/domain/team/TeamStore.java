package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.tournament.TournamentId;

import java.util.List;
import java.util.Optional;

public interface TeamStore {
    Team save(Team team);

    Optional<Team> findById(TeamId id);

    List<Team> findAllByTournamentId(TournamentId tournamentId);

    List<Team> findAllByGroupId(GroupId groupId);

    void deleteById(TeamId id);

    void deleteAllByTournamentId(TournamentId tournamentId);

    long countByOrganisationId(OrganisationId organisationId);
}
