package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.UUID;

public interface MatchStore {
    void saveAll(List<Match> matches);
    List<Match> findAllByRoundIds(List<UUID> roundIds);
    void deleteAllByRoundIds(List<UUID> roundIds);
    boolean existsResultByTournamentId(UUID tournamentId);
}
