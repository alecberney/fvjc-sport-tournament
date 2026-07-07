package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.tournament.TournamentId;

import java.util.List;

public interface RoundStore {
    void saveAll(List<Round> rounds);

    List<Round> findAllByTournamentId(TournamentId tournamentId);

    void deleteAllByTournamentId(TournamentId tournamentId);

    int countByTournamentId(TournamentId tournamentId);
}
