package abe.fvjc.tournament.tournament.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TournamentStore {
    Tournament save(Tournament tournament);
    Optional<Tournament> findById(UUID id);
    List<Tournament> findAll();
    void deleteById(UUID id);
}
