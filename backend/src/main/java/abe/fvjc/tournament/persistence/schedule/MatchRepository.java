package abe.fvjc.tournament.schedule.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findByRoundIdIn(List<UUID> roundIds);
    void deleteByRoundIdIn(List<UUID> roundIds);
}
