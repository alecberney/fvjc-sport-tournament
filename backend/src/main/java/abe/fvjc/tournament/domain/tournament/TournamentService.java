package abe.fvjc.tournament.tournament.domain;

import abe.fvjc.tournament.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.tournament.domain.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.tournament.domain.TournamentValidator.validateTournamentCreateRequest;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentStore tournamentStore;

    public Tournament create(final TournamentCreateRequest request) {
        validateTournamentCreateRequest(request);
        final var tournament = buildTournament(request);
        return tournamentStore.save(tournament);
    }

    public Tournament findById(final UUID id) {
        return tournamentStore.findById(id)
            .orElseThrow(() -> new NotFoundException("Tournament", id));
    }

    public List<Tournament> findAll() {
        return tournamentStore.findAll();
    }

    public void delete(final UUID id) {
        findById(id);
        tournamentStore.deleteById(id);
    }

    private static Tournament buildTournament(final TournamentCreateRequest request) {
        return Tournament.builder()
            .id(TournamentId.of(UUID.randomUUID()))
            .name(request.getName())
            .sport(request.getSport())
            .numberOfFields(request.getNumberOfFields())
            .minPlayersPerTeam(request.getMinPlayersPerTeam())
            .maxPlayersPerTeam(request.getMaxPlayersPerTeam())
            .date(request.getDate())
            .status(DRAFT)
            .build();
    }
}
