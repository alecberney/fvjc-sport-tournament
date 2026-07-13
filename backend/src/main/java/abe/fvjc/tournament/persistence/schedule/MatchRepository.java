package abe.fvjc.tournament.persistence.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MatchRepository extends JpaRepository<MatchEntity, UUID> {
    List<MatchEntity> findByRoundIdIn(List<UUID> roundIds);
    List<MatchEntity> findByGroupId(UUID groupId);
    void deleteByRoundIdIn(List<UUID> roundIds);
    boolean existsByRoundIdInAndResultScore1IsNotNull(List<UUID> roundIds);
}
