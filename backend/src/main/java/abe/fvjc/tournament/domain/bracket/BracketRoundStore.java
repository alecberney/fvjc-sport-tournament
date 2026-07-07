package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.tournament.TournamentId;

import java.util.List;
import java.util.Optional;

public interface BracketRoundStore {
    BracketRound save(BracketRound round);

    Optional<BracketRound> findById(BracketRoundId id);

    List<BracketRound> findAllByTournamentId(TournamentId tournamentId);

    void deleteAllByTournamentId(TournamentId tournamentId);
}
