package abe.fvjc.tournament.persistence.team;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.persistence.team.TeamDbMapper.toTeam;
import static abe.fvjc.tournament.persistence.team.TeamDbMapper.toTeamEntity;

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
    public Optional<Team> findById(final TeamId id) {
        return teamRepository.findById(id.value())
                .map(TeamDbMapper::toTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAllByTournamentId(final TournamentId tournamentId) {
        return teamRepository.findByTournamentId(tournamentId.value()).stream()
                .map(TeamDbMapper::toTeam)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Team> findAllByGroupId(final GroupId groupId) {
        return teamRepository.findByGroupId(groupId.value()).stream()
                .map(TeamDbMapper::toTeam)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(final TeamId id) {
        teamRepository.deleteById(id.value());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByOrganisationId(final OrganisationId organisationId) {
        return teamRepository.countByOrganisationId(organisationId.value());
    }
}
