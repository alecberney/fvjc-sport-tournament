package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RoundRepository extends JpaRepository<RoundEntity, UUID> {
    List<RoundEntity> findByTournamentId(UUID tournamentId);
    void deleteByTournamentId(UUID tournamentId);
    int countByTournamentId(UUID tournamentId);
}
