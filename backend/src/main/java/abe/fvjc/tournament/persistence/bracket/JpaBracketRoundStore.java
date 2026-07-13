package abe.fvjc.tournament.persistence.bracket;

import abe.fvjc.tournament.domain.bracket.BracketRound;
import abe.fvjc.tournament.domain.bracket.BracketRoundId;
import abe.fvjc.tournament.domain.bracket.BracketRoundStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public Optional<BracketRound> findById(final BracketRoundId id) {
        return bracketRoundRepository.findById(id.value())
                .map(BracketDbMapper::toBracketRound);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BracketRound> findAllByTournamentId(final TournamentId tournamentId) {
        return bracketRoundRepository.findAllByTournamentIdOrderByNumberAsc(tournamentId.value()).stream()
                .map(BracketDbMapper::toBracketRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        bracketRoundRepository.deleteAllByTournamentId(tournamentId.value());
    }
}
