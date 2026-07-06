package abe.fvjc.tournament.bracket.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BracketRoundStore {
    BracketRound save(BracketRound round);
    Optional<BracketRound> findById(UUID id);
    List<BracketRound> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
