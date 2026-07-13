package abe.fvjc.tournament.domain.tournament;

import java.util.List;
import java.util.Optional;

public interface TournamentStore {
    Tournament save(Tournament tournament);

    Optional<Tournament> findById(TournamentId id);

    List<Tournament> findAll();

    void deleteById(TournamentId id);
}
