package abe.fvjc.tournament.tournament.domain;

import abe.fvjc.tournament.bracket.domain.BracketService;
import abe.fvjc.tournament.group.domain.GroupService;
import abe.fvjc.tournament.schedule.domain.RoundStore;
import abe.fvjc.tournament.schedule.domain.ScheduleService;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.tournament.domain.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static abe.fvjc.tournament.tournament.domain.TournamentValidator.validateTournamentCreateRequest;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final RoundStore roundStore;
    private final TournamentStore tournamentStore;
    private final BracketService bracketService;
    private final GroupService groupService;
    private final ScheduleService scheduleService;
    private final TeamService teamService;

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
        bracketService.deleteAllByTournamentId(id);
        scheduleService.deleteAllByTournamentId(id);
        groupService.deleteAllByTournamentId(id);
        teamService.deleteAllByTournamentId(id);
        tournamentStore.deleteById(id);
    }

    public Tournament start(final UUID id) {
        final var tournament = tournamentStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Tournament", id));
        if (tournament.getStatus() == IN_PROGRESS) {
            throw new ConflictException("Le tournoi est déjà démarré");
        }
        if (roundStore.countByTournamentId(id) == 0) {
            throw new ConflictException("Impossible de démarrer le tournoi sans calendrier généré");
        }
        return tournamentStore.save(tournament.withStatus(IN_PROGRESS));
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
