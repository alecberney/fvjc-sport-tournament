package abe.fvjc.tournament.bracket.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BracketRoundRepository extends JpaRepository<BracketRoundEntity, UUID> {
    List<BracketRoundEntity> findAllByTournamentIdOrderByNumberAsc(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
