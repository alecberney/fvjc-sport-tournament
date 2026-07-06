package abe.fvjc.tournament.bracket.persistence;

import abe.fvjc.tournament.bracket.domain.BracketRound;
import abe.fvjc.tournament.bracket.domain.BracketRoundStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaBracketRoundStore implements BracketRoundStore {
    private final BracketRoundRepository bracketRoundRepository;

    @Override
    @Transactional
    public BracketRound save(final BracketRound round) {
        final var entity = BracketDbMapper.toBracketRoundEntity(round);
        final var savedEntity = bracketRoundRepository.save(entity);
        return BracketDbMapper.toBracketRound(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BracketRound> findById(final UUID id) {
        return bracketRoundRepository.findById(id)
                .map(BracketDbMapper::toBracketRound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketRound> findAllByTournamentId(final UUID tournamentId) {
        return bracketRoundRepository.findAllByTournamentIdOrderByNumberAsc(tournamentId).stream()
                .map(BracketDbMapper::toBracketRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        bracketRoundRepository.deleteAllByTournamentId(tournamentId);
    }
}
