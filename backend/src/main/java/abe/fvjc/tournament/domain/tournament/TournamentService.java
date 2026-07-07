package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.schedule.RoundStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static abe.fvjc.tournament.domain.tournament.TournamentValidator.validateTournamentCreateRequest;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final RoundStore roundStore;
    private final TournamentStore tournamentStore;
    private final TournamentSearchService tournamentSearchService;

    public Tournament create(final TournamentCreateRequest request) {
        validateTournamentCreateRequest(request);

        final var tournament = buildTournament(request);
        return tournamentStore.save(tournament);
    }

    public void delete(final UUID id) {
        tournamentSearchService.findById(id);
        tournamentStore.deleteById(TournamentId.of(id));
    }

    public Tournament start(final UUID id) {
        final var tournament = tournamentSearchService.findById(id);

        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new ConflictException("Le tournoi est déjà démarré");
        }
        if (roundStore.countByTournamentId(TournamentId.of(id)) == 0) {
            throw new ConflictException("Impossible de démarrer le tournoi sans calendrier généré");
        }

        final var tournamentInProgress = tournament.withStatus(TournamentStatus.IN_PROGRESS);
        return tournamentStore.save(tournamentInProgress);
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
                .status(TournamentStatus.DRAFT)
                .build();
    }
}
