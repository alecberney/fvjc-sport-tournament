package abe.fvjc.tournament.schedule.domain;

import java.util.List;
import java.util.UUID;

public interface RoundStore {
    void saveAll(List<Round> rounds);
    List<Round> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
    int countByTournamentId(UUID tournamentId);
}
