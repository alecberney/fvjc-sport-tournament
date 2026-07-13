package abe.fvjc.tournament.persistence.schedule;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.schedule.Match;
import abe.fvjc.tournament.domain.schedule.MatchId;
import abe.fvjc.tournament.domain.schedule.MatchStore;
import abe.fvjc.tournament.domain.schedule.RoundId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
    public Optional<Match> findById(final MatchId matchId) {
        return matchRepository.findById(matchId.value())
                .map(MatchDbMapper::toMatch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByRoundIds(final List<RoundId> roundIds) {
        final var rawRoundIds = roundIds.stream()
                .map(RoundId::value)
                .toList();
        return matchRepository.findByRoundIdIn(rawRoundIds).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Match> findAllByGroupId(final GroupId groupId) {
        return matchRepository.findByGroupId(groupId.value()).stream()
                .map(MatchDbMapper::toMatch)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAllByRoundIds(final List<RoundId> roundIds) {
        final var rawRoundIds = roundIds.stream()
                .map(RoundId::value)
                .toList();
        matchRepository.deleteByRoundIdIn(rawRoundIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsResultByRoundIds(final List<RoundId> roundIds) {
        final var rawRoundIds = roundIds.stream()
                .map(RoundId::value)
                .toList();
        return matchRepository.existsByRoundIdInAndResultScore1IsNotNull(rawRoundIds);
    }
}
