package abe.fvjc.tournament.group.persistence;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class GroupDbMapper {

    static Group toGroup(final GroupEntity entity) {
        return Group.builder()
            .id(GroupId.of(entity.getId()))
            .name(entity.getName())
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static GroupEntity toGroupEntity(final Group group) {
        final var entity = new GroupEntity();
        final var id = group.getId().isEmpty()
                ? UUID.randomUUID()
                : group.getId().value();
        entity.setId(id);
        entity.setName(group.getName());
        entity.setTournamentId(group.getTournamentId().value());
        return entity;
    }
}
