package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.team.domain.IdGenerator.organisationId;
import static abe.fvjc.tournament.team.domain.TeamFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private OrganisationStore organisationStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private TeamService teamService;

    @Test
    void registerTeamsWhenCount1ShouldCreateOneTeamWithExactName() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(1, teamsCreated.size());
        assertEquals("Les Aigles", teamsCreated.get(0).getName());
    }

    @Test
    void registerTeamsWhenCountGreaterThan1ShouldAppendNumbers() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequestWithCount(3);

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore, times(3)).save(any(Team.class));

        assertEquals(3, teamsCreated.size());
        assertEquals("Les Aigles 1", teamsCreated.get(0).getName());
        assertEquals("Les Aigles 2", teamsCreated.get(1).getName());
        assertEquals("Les Aigles 3", teamsCreated.get(2).getName());
    }

    @Test
    void registerTeamsWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.registerTeams(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void registerTeamsWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();
        final var request = buildRegisterRequest();

        when(tournamentStore.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> teamService.registerTeams(id, request));

        verify(tournamentStore).findById(id);
    }

    @Test
    void findAllByTournamentIdShouldReturnTeamViewsWithOrganisationData() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(team));
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));

        final var teamsFound = teamService.findAllByTournamentId(tournament.getId().value());

        verify(teamStore).findAllByTournamentId(tournament.getId().value());
        verify(organisationStore).findById(org.getId().value());

        assertEquals(1, teamsFound.size());
        assertEquals(team.getName(), teamsFound.get(0).getName());
        assertEquals(org.getResponsible().getFirstName(), teamsFound.get(0).getResponsible().getFirstName());
    }

    @Test
    void updateTeamWhenValidShouldUpdateNameAndResponsible() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());
        final var request = buildUpdateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamUpdated = teamService.updateTeam(tournament.getId().value(), team.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(team.getId().value());
        verify(organisationStore).findById(org.getId().value());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(request.getName(), teamUpdated.getName());
        assertEquals(request.getResponsible().getFirstName(), teamUpdated.getResponsible().getFirstName());
    }

    @Test
    void updateTeamWhenTeamNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament();
        final var teamId = IdGenerator.teamId().value();
        final var request = buildUpdateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(teamId)).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
            () -> teamService.updateTeam(tournament.getId().value(), teamId, request));

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(teamId);

        assertEquals("Team not found with id: " + teamId, exception.getMessage());
    }

    @Test
    void updateTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = IdGenerator.teamId().value();
        final var request = buildUpdateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.updateTeam(tournament.getId().value(), teamId, request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void deleteTeamWhenLastInOrgShouldDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = organisationId();
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(orgId.value())).thenReturn(0L);

        teamService.deleteTeam(tournament.getId().value(), team.getId().value());

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(team.getId().value());
        verify(teamStore).deleteById(team.getId().value());
        verify(teamStore).countByOrganisationId(orgId.value());
        verify(organisationStore).deleteById(orgId.value());
    }

    @Test
    void deleteTeamWhenNotLastInOrgShouldNotDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = organisationId();
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(orgId.value())).thenReturn(1L);

        teamService.deleteTeam(tournament.getId().value(), team.getId().value());

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findById(team.getId().value());
        verify(teamStore).deleteById(team.getId().value());
        verify(teamStore).countByOrganisationId(orgId.value());
        verify(organisationStore, never()).deleteById(any());
    }

    @Test
    void deleteTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = IdGenerator.teamId().value();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> teamService.deleteTeam(tournament.getId().value(), teamId));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void markPaidWhenExistsShouldUpdatePaidStatus() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamStore.findById(team.getId().value())).thenReturn(Optional.of(team));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(organisationStore.findById(org.getId().value())).thenReturn(Optional.of(org));

        final var teamUpdated = teamService.markPaid(team.getId().value(), true);

        verify(teamStore).findById(team.getId().value());
        verify(teamStore).save(any(Team.class));
        verify(organisationStore).findById(org.getId().value());

        assertTrue(teamUpdated.isPaid());
    }

    @Test
    void markPaidWhenNotFoundShouldThrowNotFoundException() {
        final var teamId = IdGenerator.teamId().value();

        when(teamStore.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> teamService.markPaid(teamId, true));

        verify(teamStore).findById(teamId);
    }

    @Test
    void deleteAllByTournamentIdShouldDeleteTeamsThenOrganisations() {
        final var tournamentId = UUID.randomUUID();

        teamService.deleteAllByTournamentId(tournamentId);

        verify(teamStore).deleteAllByTournamentId(tournamentId);
        verify(organisationStore).deleteAllByTournamentId(tournamentId);
    }
}
