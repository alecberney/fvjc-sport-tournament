package abe.fvjc.tournament.team.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface TeamRepository extends JpaRepository<TeamEntity, UUID> {
    List<TeamEntity> findByTournamentId(UUID tournamentId);
    List<TeamEntity> findByGroupId(UUID groupId);
    long countByOrganisationId(UUID organisationId);
}
