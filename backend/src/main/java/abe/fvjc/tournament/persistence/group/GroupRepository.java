package abe.fvjc.tournament.group.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GroupRepository extends JpaRepository<GroupEntity, UUID> {
    List<GroupEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
}
