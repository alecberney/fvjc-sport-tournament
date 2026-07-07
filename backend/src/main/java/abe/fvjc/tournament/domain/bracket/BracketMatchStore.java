package abe.fvjc.tournament.domain.bracket;

import java.util.List;
import java.util.Optional;

public interface BracketMatchStore {
    BracketMatch save(BracketMatch match);

    Optional<BracketMatch> findById(BracketMatchId id);

    List<BracketMatch> findAllByRoundId(BracketRoundId roundId);

    void deleteAllByRoundId(BracketRoundId roundId);
}
