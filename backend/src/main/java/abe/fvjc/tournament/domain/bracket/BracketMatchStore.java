package abe.fvjc.tournament.bracket.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BracketMatchStore {
    BracketMatch save(BracketMatch match);
    Optional<BracketMatch> findById(UUID id);
    List<BracketMatch> findAllByRoundId(UUID roundId);
    void deleteAllByRoundId(UUID roundId);
}
