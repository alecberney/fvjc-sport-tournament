package abe.fvjc.tournament.group.persistence;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.group.persistence.GroupDbMapper.toGroup;
import static abe.fvjc.tournament.group.persistence.GroupDbMapper.toGroupEntity;

@Repository
@RequiredArgsConstructor
class JpaGroupStore implements GroupStore {
    private final GroupRepository groupRepository;

    @Override
    @Transactional
    public Group save(final Group group) {
        final var entity = toGroupEntity(group);
        final var savedEntity = groupRepository.save(entity);
        return toGroup(savedEntity);
    }

    @Override
    @Transactional
    public void saveAll(final List<Group> groups) {
        final var entities = groups.stream()
            .map(GroupDbMapper::toGroupEntity)
            .toList();
        groupRepository.saveAll(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Group> findAllByTournamentId(final UUID tournamentId) {
        return groupRepository.findByTournamentId(tournamentId).stream()
            .map(GroupDbMapper::toGroup)
            .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final UUID tournamentId) {
        groupRepository.deleteByTournamentId(tournamentId);
    }
}
