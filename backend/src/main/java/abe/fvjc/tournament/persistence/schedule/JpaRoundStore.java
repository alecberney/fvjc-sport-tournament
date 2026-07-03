package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Round;
import abe.fvjc.tournament.schedule.domain.RoundStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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
    public List<Round> findAllByTournamentId(final UUID tournamentId) {
        return roundRepository.findByTournamentId(tournamentId).stream()
                .map(RoundDbMapper::toRound)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        roundRepository.deleteByTournamentId(tournamentId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countByTournamentId(final UUID tournamentId) {
        return roundRepository.countByTournamentId(tournamentId);
    }
}
