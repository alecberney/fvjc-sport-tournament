package abe.fvjc.tournament.team.persistence;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.team.persistence.TeamDbMapper.toTeam;
import static abe.fvjc.tournament.team.persistence.TeamDbMapper.toTeamEntity;

@Repository
@RequiredArgsConstructor
class JpaTeamStore implements TeamStore {
    private final TeamRepository teamRepository;

    @Override
    @Transactional
    public Team save(final Team team) {
        final var entity = toTeamEntity(team);
        final var savedEntity = teamRepository.save(entity);
        return toTeam(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Team> findById(final UUID id) {
        return teamRepository.findById(id)
                .map(TeamDbMapper::toTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAllByTournamentId(final UUID tournamentId) {
        return teamRepository.findByTournamentId(tournamentId).stream()
                .map(TeamDbMapper::toTeam)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(final UUID id) {
        teamRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrganisationId(final UUID organisationId) {
        return teamRepository.countByOrganisationId(organisationId);
    }
}
