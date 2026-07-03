package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class JpaMatchStore implements MatchStore {
    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public void saveAll(final List<Match> matches) {
        final var entities = matches.stream()
                .map(MatchDbMapper::toMatchEntity)
                .toList();
        matchRepository.saveAll(entities);
    }

    @Override
    @Transactional
    public Match save(final Match match) {
        final var entity = MatchDbMapper.toMatchEntity(match);
        final var savedEntity = matchRepository.save(entity);
        return MatchDbMapper.toMatch(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Match> findById(final UUID matchId) {
        return matchRepository.findById(matchId)
                .map(MatchDbMapper::toMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByRoundIds(final List<UUID> roundIds) {
        return matchRepository.findByRoundIdIn(roundIds).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByGroupId(final UUID groupId) {
        return matchRepository.findByGroupId(groupId).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundIds(final List<UUID> roundIds) {
        matchRepository.deleteByRoundIdIn(roundIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsResultByRoundIds(final List<UUID> roundIds) {
        return matchRepository.existsByRoundIdInAndResultScore1IsNotNull(roundIds);
    }
}
