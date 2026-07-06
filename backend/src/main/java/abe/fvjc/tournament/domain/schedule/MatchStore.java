package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchStore {
    void saveAll(List<Match> matches);
    Match save(Match match);
    Optional<Match> findById(UUID matchId);
    List<Match> findAllByRoundIds(List<UUID> roundIds);
    List<Match> findAllByGroupId(UUID groupId);
    void deleteAllByRoundIds(List<UUID> roundIds);
    boolean existsResultByRoundIds(List<UUID> roundIds);
}
