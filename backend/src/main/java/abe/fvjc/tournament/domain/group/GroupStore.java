package abe.fvjc.tournament.group.domain;

import java.util.List;
import java.util.UUID;

public interface GroupStore {
    Group save(Group group);
    void saveAll(List<Group> groups);
    List<Group> findAllByTournamentId(UUID tournamentId);
    void deleteAllByTournamentId(UUID tournamentId);
}
