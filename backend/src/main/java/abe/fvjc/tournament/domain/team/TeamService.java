package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static abe.fvjc.tournament.team.domain.TeamValidator.validateTeamRegisterRequest;

@Service
@RequiredArgsConstructor
public class TeamService {
    private final OrganisationStore organisationStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public List<TeamOverview> registerTeams(final UUID tournamentId, final TeamRegisterRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        validateTeamRegisterRequest(request);
        final var org = organisationStore.save(Organisation.builder()
            .id(OrganisationId.of(UUID.randomUUID()))
            .responsible(request.getResponsible())
            .tournamentId(TournamentId.of(tournamentId))
            .build());
        return IntStream.rangeClosed(1, request.getCount())
            .mapToObj(i -> {
                final var name = request.getCount() == 1
                    ? request.getName()
                    : request.getName() + " " + i;
                final var team = teamStore.save(Team.builder()
                    .id(TeamId.of(UUID.randomUUID()))
                    .name(name)
                    .paid(request.getPaid().get(i - 1))
                    .organisationId(org.getId())
                    .tournamentId(TournamentId.of(tournamentId))
                    .build());
                return buildTeamOverview(team, org);
            })
            .toList();
    }

    public List<TeamOverview> findAllByTournamentId(final UUID tournamentId) {
        return teamStore.findAllByTournamentId(tournamentId).stream()
            .map(team -> {
                final var org = organisationStore.findById(team.getOrganisationId().value())
                    .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
                return buildTeamOverview(team, org);
            })
            .toList();
    }

    public TeamOverview updateTeam(final UUID tournamentId, final UUID teamId, final TeamUpdateRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var org = organisationStore.findById(team.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", team.getOrganisationId().value()));
        final var updatedOrg = organisationStore.save(org.withResponsible(request.getResponsible()));
        final var updatedTeam = teamStore.save(team
            .withName(request.getName())
            .withPaid(request.isPaid()));
        return buildTeamOverview(updatedTeam, updatedOrg);
    }

    public void deleteTeam(final UUID tournamentId, final UUID teamId) {
        final var tournament = tournamentStore.findById(tournamentId)
            .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        assertDraft(tournament.getStatus());
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        teamStore.deleteById(teamId);
        final var remaining = teamStore.countByOrganisationId(team.getOrganisationId().value());
        if (remaining == 0) {
            organisationStore.deleteById(team.getOrganisationId().value());
        }
    }

    public void deleteAllByTournamentId(final UUID tournamentId) {
        teamStore.deleteAllByTournamentId(tournamentId);
        organisationStore.deleteAllByTournamentId(tournamentId);
    }

    public TeamOverview markPaid(final UUID teamId, final boolean paid) {
        final var team = teamStore.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team", teamId));
        final var updatedTeam = teamStore.save(team.withPaid(paid));
        final var org = organisationStore.findById(updatedTeam.getOrganisationId().value())
            .orElseThrow(() -> new NotFoundException("Organisation", updatedTeam.getOrganisationId().value()));
        return buildTeamOverview(updatedTeam, org);
    }

    private static void assertDraft(final TournamentStatus status) {
        if (status != TournamentStatus.DRAFT) {
            throw new ConflictException(
                "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation");
        }
    }

    private static TeamOverview buildTeamOverview(final Team team, final Organisation org) {
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
