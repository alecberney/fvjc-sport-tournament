package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.Organisation;
import abe.fvjc.tournament.domain.organisation.OrganisationService;
import abe.fvjc.tournament.domain.organisation.OrganisationStore;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.team.TeamValidator.validateCurrentStatusIsDraft;
import static abe.fvjc.tournament.domain.team.TeamValidator.validateTeamRegisterRequest;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final OrganisationStore organisationStore;
    private final TeamStore teamStore;
    private final OrganisationService organisationService;
    private final TournamentSearchService tournamentSearchService;

    public List<Team> registerTeams(final UUID tournamentId, final TeamRegisterRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        validateCurrentStatusIsDraft(tournament.getStatus());
        validateTeamRegisterRequest(request);

        final var organisationSaved = organisationService.create(request.getResponsible(), tournament.getId());

        final var teams = new ArrayList<Team>();
        for (int teamNumber = 1; teamNumber <= request.getCount(); teamNumber++) {
            final var team = createTeam(tournament.getId(), request, teamNumber, organisationSaved);
            teams.add(team);
        }
        return teams;
    }

    private Team createTeam(
            final TournamentId tournamentId,
            final TeamRegisterRequest request,
            final int teamNumber,
            final Organisation organisationSaved) {
        final var name = request.getCount() == 1
            ? request.getName()
            : request.getName() + " " + teamNumber;
        final var build = buildTeam(tournamentId, request, teamNumber, name, organisationSaved);
        return teamStore.save(build);
    }

    public List<Team> findAllByTournamentId(final UUID tournamentId) {
        return teamStore.findAllByTournamentId(TournamentId.of(tournamentId));
    }

    public Team updateTeam(final UUID tournamentId, final UUID teamId, final TeamUpdateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        validateCurrentStatusIsDraft(tournament.getStatus());

        final var team = findById(teamId);

        organisationService.updateResponsible(request.getResponsible(), team.getOrganisationId());

        final var updatedTeam = team.withName(request.getName())
                .withPaid(request.isPaid());
        return teamStore.save(updatedTeam);
    }

    public void deleteTeam(final UUID tournamentId, final UUID teamId) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        validateCurrentStatusIsDraft(tournament.getStatus());

        final var team = findById(teamId);
        teamStore.deleteById(TeamId.of(teamId));

        final var remaining = teamStore.countByOrganisationId(team.getOrganisationId());
        if (remaining == 0) {
            organisationStore.deleteById(team.getOrganisationId());
        }
    }

    public Team markPaid(final UUID teamId, final boolean paid) {
        final var team = findById(teamId);
        final var teamPaid = team.withPaid(paid);
        return teamStore.save(teamPaid);
    }

    private Team findById(UUID teamId) {
        return teamStore.findById(TeamId.of(teamId))
                .orElseThrow(() -> new NotFoundException("Team", teamId));
    }

    private static Team buildTeam(
            final TournamentId tournamentId,
            final TeamRegisterRequest request,
            final int i,
            final String name,
            final Organisation org) {
        return Team.builder()
                .id(TeamId.of(UUID.randomUUID()))
                .name(name)
                .paid(request.getPaid().get(i - 1))
                .organisationId(org.getId())
                .tournamentId(tournamentId)
                .build();
    }
}
