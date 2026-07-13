package abe.fvjc.tournament.persistence.group;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static abe.fvjc.tournament.persistence.group.GroupDbMapper.toGroup;
import static abe.fvjc.tournament.persistence.group.GroupDbMapper.toGroupEntity;

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
    public List<Group> findAllByTournamentId(final TournamentId tournamentId) {
        return groupRepository.findByTournamentId(tournamentId.value()).stream()
            .map(GroupDbMapper::toGroup)
            .toList();
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        groupRepository.deleteByTournamentId(tournamentId.value());
    }
}
