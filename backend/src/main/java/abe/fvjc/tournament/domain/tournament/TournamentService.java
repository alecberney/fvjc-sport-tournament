package abe.fvjc.tournament.domain.tournament;

import abe.fvjc.tournament.domain.bracket.BracketService;
import abe.fvjc.tournament.domain.group.GroupService;
import abe.fvjc.tournament.domain.schedule.RoundStore;
import abe.fvjc.tournament.domain.schedule.ScheduleService;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.team.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.tournament.TournamentStatus.DRAFT;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static abe.fvjc.tournament.domain.tournament.TournamentValidator.validateTournamentCreateRequest;

@Service
@RequiredArgsConstructor
public class TournamentService {
    private final RoundStore roundStore;
    private final TournamentStore tournamentStore;
    private final BracketService bracketService;
    private final GroupService groupService;
    private final ScheduleService scheduleService;
    private final TeamService teamService;
    private final TournamentSearchService tournamentSearchService;

    public Tournament create(final TournamentCreateRequest request) {
        validateTournamentCreateRequest(request);
        final var tournament = buildTournament(request);
        return tournamentStore.save(tournament);
    }

    public Tournament findById(final TournamentId id) {
        return tournamentSearchService.findById(id);
    }

    public List<Tournament> findAll() {
        return tournamentStore.findAll();
    }

    public void delete(final TournamentId id) {
        findById(id);
        bracketService.deleteAllByTournamentId(id);
        scheduleService.deleteAllByTournamentId(id);
        groupService.deleteAllByTournamentId(id);
        teamService.deleteAllByTournamentId(id);
        tournamentStore.deleteById(id);
    }

    public Tournament start(final TournamentId id) {
        final var tournament = tournamentSearchService.findById(id);
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
