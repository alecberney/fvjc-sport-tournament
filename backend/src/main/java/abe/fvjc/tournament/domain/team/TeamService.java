package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.Organisation;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.organisation.OrganisationSearchService;
import abe.fvjc.tournament.domain.organisation.OrganisationStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static abe.fvjc.tournament.domain.team.TeamValidator.validateTeamRegisterRequest;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final OrganisationStore organisationStore;
    private final TeamStore teamStore;
    private final OrganisationSearchService organisationSearchService;
    private final TeamSearchService teamSearchService;
    private final TournamentSearchService tournamentSearchService;

    public List<TeamOverview> registerTeams(final TournamentId tournamentId, final TeamRegisterRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        assertDraft(tournament.getStatus());
        validateTeamRegisterRequest(request);
        final var org = organisationStore.save(buildOrganisation(tournamentId, request));
        return IntStream.rangeClosed(1, request.getCount())
            .mapToObj(i -> {
                final var name = request.getCount() == 1
                    ? request.getName()
                    : request.getName() + " " + i;
                final var team = teamStore.save(
                    buildTeam(tournamentId, org.getId(), name, request.getPaid().get(i - 1)));
                return toTeamOverview(team, org);
            })
            .toList();
    }

    public List<TeamOverview> findAllByTournamentId(final TournamentId tournamentId) {
        return teamStore.findAllByTournamentId(tournamentId).stream()
            .map(team -> {
                final var org = organisationSearchService.findById(team.getOrganisationId());
                return toTeamOverview(team, org);
            })
            .toList();
    }

    public TeamOverview updateTeam(final TournamentId tournamentId, final TeamId teamId, final TeamUpdateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        assertDraft(tournament.getStatus());
        final var team = teamSearchService.findById(teamId);
        final var org = organisationSearchService.findById(team.getOrganisationId());
        final var updatedOrg = organisationStore.save(org.withResponsible(request.getResponsible()));
        final var updatedTeam = teamStore.save(team
            .withName(request.getName())
            .withPaid(request.isPaid()));
        return toTeamOverview(updatedTeam, updatedOrg);
    }

    public void deleteTeam(final TournamentId tournamentId, final TeamId teamId) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        assertDraft(tournament.getStatus());
        final var team = teamSearchService.findById(teamId);
        teamStore.deleteById(teamId);
        final var remaining = teamStore.countByOrganisationId(team.getOrganisationId());
        if (remaining == 0) {
            organisationStore.deleteById(team.getOrganisationId());
        }
    }

    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        teamStore.deleteAllByTournamentId(tournamentId);
        organisationStore.deleteAllByTournamentId(tournamentId);
    }

    public TeamOverview markPaid(final TeamId teamId, final boolean paid) {
        final var team = teamSearchService.findById(teamId);
        final var updatedTeam = teamStore.save(team.withPaid(paid));
        final var org = organisationSearchService.findById(updatedTeam.getOrganisationId());
        return toTeamOverview(updatedTeam, org);
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation");
        }
    }

    private static Organisation buildOrganisation(final TournamentId tournamentId, final TeamRegisterRequest request) {
        return Organisation.builder()
            .id(OrganisationId.of(UUID.randomUUID()))
            .responsible(request.getResponsible())
            .tournamentId(tournamentId)
            .build();
    }

    private static Team buildTeam(
            final TournamentId tournamentId,
            final OrganisationId organisationId,
            final String name,
            final boolean paid) {
        return Team.builder()
            .id(TeamId.of(UUID.randomUUID()))
            .name(name)
            .paid(paid)
            .organisationId(organisationId)
            .tournamentId(tournamentId)
            .build();
    }

    private static TeamOverview toTeamOverview(final Team team, final Organisation org) {
        return TeamOverview.builder()
            .id(team.getId())
            .name(team.getName())
            .paid(team.isPaid())
            .organisationId(team.getOrganisationId())
            .responsible(org.getResponsible())
            .tournamentId(team.getTournamentId())
            .build();
    }
}
