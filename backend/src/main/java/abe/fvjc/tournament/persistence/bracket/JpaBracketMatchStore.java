package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketMatch;
import abe.fvjc.tournament.bracket.domain.BracketMatchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<BracketMatch> findById(final UUID id) {
        return bracketMatchRepository.findById(id)
                .map(BracketDbMapper::toBracketMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketMatch> findAllByRoundId(final UUID roundId) {
        return bracketMatchRepository.findAllByRoundId(roundId).stream()
                .map(BracketDbMapper::toBracketMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundId(final UUID roundId) {
        bracketMatchRepository.deleteAllByRoundId(roundId);
    }
}
