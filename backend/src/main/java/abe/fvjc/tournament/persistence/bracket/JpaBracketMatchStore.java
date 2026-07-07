package abe.fvjc.tournament.persistence.bracket;

import abe.fvjc.tournament.domain.bracket.BracketMatch;
import abe.fvjc.tournament.domain.bracket.BracketMatchId;
import abe.fvjc.tournament.domain.bracket.BracketMatchStore;
import abe.fvjc.tournament.domain.bracket.BracketRoundId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
class JpaBracketMatchStore implements BracketMatchStore {
    private final BracketMatchRepository bracketMatchRepository;

    @Override
    @Transactional
    public BracketMatch save(final BracketMatch match) {
        final var entity = BracketDbMapper.toBracketMatchEntity(match);
        final var savedEntity = bracketMatchRepository.save(entity);
        return BracketDbMapper.toBracketMatch(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BracketMatch> findById(final BracketMatchId id) {
        return bracketMatchRepository.findById(id.value())
                .map(BracketDbMapper::toBracketMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketMatch> findAllByRoundId(final BracketRoundId roundId) {
        return bracketMatchRepository.findAllByRoundId(roundId.value()).stream()
                .map(BracketDbMapper::toBracketMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundId(final BracketRoundId roundId) {
        bracketMatchRepository.deleteAllByRoundId(roundId.value());
    }
}
