package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.tournament.TournamentId;

import java.util.List;

public interface GroupStore {
    Group save(Group group);

    void saveAll(List<Group> groups);

    List<Group> findAllByTournamentId(TournamentId tournamentId);

    void deleteAllByTournamentId(TournamentId tournamentId);
}
