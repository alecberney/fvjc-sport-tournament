package abe.fvjc.tournament.bracket.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BracketMatchRepository extends JpaRepository<BracketMatchEntity, UUID> {
    List<BracketMatchEntity> findAllByRoundId(UUID roundId);
    void deleteAllByRoundId(UUID roundId);
}
