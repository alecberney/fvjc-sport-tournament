package abe.fvjc.tournament.persistence.schedule;

import abe.fvjc.tournament.domain.schedule.Round;
import abe.fvjc.tournament.domain.schedule.RoundStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
class JpaRoundStore implements RoundStore {
    private final RoundRepository roundRepository;

    @Override
    @Transactional
    public void saveAll(final List<Round> rounds) {
        final var entities = rounds.stream()
                .map(RoundDbMapper::toRoundEntity)
                .toList();
        roundRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Round> findAllByTournamentId(final TournamentId tournamentId) {
        return roundRepository.findByTournamentId(tournamentId.value()).stream()
                .map(RoundDbMapper::toRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        roundRepository.deleteByTournamentId(tournamentId.value());
    }

    @Override
    @Transactional(readOnly = true)
    public int countByTournamentId(final TournamentId tournamentId) {
        return roundRepository.countByTournamentId(tournamentId.value());
    }
}
