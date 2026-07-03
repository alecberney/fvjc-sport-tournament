package abe.fvjc.tournament.team.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamStore {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findAllByTournamentId(UUID tournamentId);
    List<Team> findAllByGroupId(UUID groupId);
    void deleteById(UUID id);
    long countByOrganisationId(UUID organisationId);
}
