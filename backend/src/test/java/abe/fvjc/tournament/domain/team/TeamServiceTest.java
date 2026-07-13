package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.Organisation;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.organisation.OrganisationSearchService;
import abe.fvjc.tournament.domain.organisation.OrganisationStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildOrganisationForTournament;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildRegisterRequest;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildRegisterRequestWithCount;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildUpdateRequest;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private OrganisationStore organisationStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private OrganisationSearchService organisationSearchService;

    @Mock
    private TeamSearchService teamSearchService;

    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private TeamService teamService;

    @Test
    void registerTeamsWhenCount1ShouldCreateOneTeamWithExactName() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournament.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(1, teamsCreated.size());
        assertEquals("Les Aigles", teamsCreated.get(0).getName());
    }

    @Test
    void registerTeamsWhenCountGreaterThan1ShouldAppendNumbers() {
        final var tournament = buildTournament();
        final var request = buildRegisterRequestWithCount(3);

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournament.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
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

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> teamService.registerTeams(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation",
            exception.getMessage());
    }

    @Test
    void findAllByTournamentIdShouldReturnTeamViewsWithOrganisationData() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(team));
        when(organisationSearchService.findById(org.getId())).thenReturn(org);

        final var teamsFound = teamService.findAllByTournamentId(tournament.getId());

        verify(teamStore).findAllByTournamentId(tournament.getId());
        verify(organisationSearchService).findById(org.getId());

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

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamSearchService.findById(team.getId())).thenReturn(team);
        when(organisationSearchService.findById(org.getId())).thenReturn(org);
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamUpdated = teamService.updateTeam(tournament.getId(), team.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(teamSearchService).findById(team.getId());
        verify(organisationSearchService).findById(org.getId());
        verify(organisationStore).save(any(Organisation.class));
        verify(teamStore).save(any(Team.class));

        assertEquals(request.getName(), teamUpdated.getName());
        assertEquals(request.getResponsible().getFirstName(), teamUpdated.getResponsible().getFirstName());
    }

    @Test
    void updateTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = TeamId.of(UUID.randomUUID());
        final var request = buildUpdateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> teamService.updateTeam(tournament.getId(), teamId, request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation",
            exception.getMessage());
    }

    @Test
    void deleteTeamWhenLastInOrgShouldDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = OrganisationId.of(UUID.randomUUID());
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamSearchService.findById(team.getId())).thenReturn(team);
        when(teamStore.countByOrganisationId(orgId)).thenReturn(0L);

        teamService.deleteTeam(tournament.getId(), team.getId());

        verify(tournamentSearchService).findById(tournament.getId());
        verify(teamSearchService).findById(team.getId());
        verify(teamStore).deleteById(team.getId());
        verify(teamStore).countByOrganisationId(orgId);
        verify(organisationStore).deleteById(orgId);
    }

    @Test
    void deleteTeamWhenNotLastInOrgShouldNotDeleteOrg() {
        final var tournament = buildTournament();
        final var orgId = OrganisationId.of(UUID.randomUUID());
        final var team = buildTeam(orgId, tournament.getId());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamSearchService.findById(team.getId())).thenReturn(team);
        when(teamStore.countByOrganisationId(orgId)).thenReturn(1L);

        teamService.deleteTeam(tournament.getId(), team.getId());

        verify(tournamentSearchService).findById(tournament.getId());
        verify(teamSearchService).findById(team.getId());
        verify(teamStore).deleteById(team.getId());
        verify(teamStore).countByOrganisationId(orgId);
        verify(organisationStore, never()).deleteById(any());
    }

    @Test
    void deleteTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var teamId = TeamId.of(UUID.randomUUID());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> teamService.deleteTeam(tournament.getId(), teamId));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation",
            exception.getMessage());
    }

    @Test
    void markPaidWhenExistsShouldUpdatePaidStatus() {
        final var tournament = buildTournament();
        final var org = buildOrganisationForTournament(tournament.getId());
        final var team = buildTeam(org.getId(), tournament.getId());

        when(teamSearchService.findById(team.getId())).thenReturn(team);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(organisationSearchService.findById(org.getId())).thenReturn(org);

        final var teamUpdated = teamService.markPaid(team.getId(), true);

        verify(teamSearchService).findById(team.getId());
        verify(teamStore).save(any(Team.class));
        verify(organisationSearchService).findById(org.getId());

        assertTrue(teamUpdated.isPaid());
    }

    @Test
    void deleteAllByTournamentIdShouldDeleteTeamsThenOrganisations() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());

        teamService.deleteAllByTournamentId(tournamentId);

        verify(teamStore).deleteAllByTournamentId(tournamentId);
        verify(organisationStore).deleteAllByTournamentId(tournamentId);
    }
}
