package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.OrganisationService;
import abe.fvjc.tournament.domain.organisation.OrganisationStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildRegisterRequest;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildRegisterRequestWithCount;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildUpdateRequest;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
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
    private OrganisationService organisationService;
    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private TeamService teamService;

    @Test
    void registerTeamsWhenCount1ShouldCreateOneTeamWithExactName() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildRegisterRequest();
        final var person = request.getResponsible();
        final var organisation = buildOrganisation();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(organisationService.create(person, tournamentId)).thenReturn(organisation);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(organisationService).create(person, tournamentId);
        verify(teamStore).save(any(Team.class));

        assertEquals(1, teamsCreated.size());
        assertEquals("Les Aigles", teamsCreated.getFirst().getName());
    }

    @Test
    void registerTeamsWhenCountGreaterThan1ShouldAppendNumbers() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildRegisterRequestWithCount(3);
        final var person = request.getResponsible();
        final var organisation = buildOrganisation();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(organisationService.create(person, tournamentId)).thenReturn(organisation);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamsCreated = teamService.registerTeams(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(organisationService).create(person, tournamentId);
        verify(teamStore, times(3)).save(any(Team.class));

        assertEquals(3, teamsCreated.size());
        assertEquals("Les Aigles 1", teamsCreated.get(0).getName());
        assertEquals("Les Aigles 2", teamsCreated.get(1).getName());
        assertEquals("Les Aigles 3", teamsCreated.get(2).getName());
    }

    @Test
    void registerTeamsWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentId = tournament.getId().value();
        final var request = buildRegisterRequest();

        when(tournamentSearchService.findById(tournamentId)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> teamService.registerTeams(tournamentId, request));

        verify(tournamentSearchService).findById(tournamentId);

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation", exception.getMessage());
    }

    @Test
    void findAllByTournamentIdShouldReturnTeamViewsWithOrganisationData() {
        final var organisation = buildOrganisation();
        final var organisationId = organisation.getId();
        final var tournamentId = organisation.getTournamentId();
        final var team = buildTeam(organisationId, tournamentId);

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(team));

        final var teamsFound = teamService.findAllByTournamentId(tournamentId.value());

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals(1, teamsFound.size());
        assertEquals(team.getName(), teamsFound.getFirst().getName());
    }

    @Test
    void updateTeamWhenValidShouldUpdateNameAndResponsible() {
        final var organisation = buildOrganisation();
        final var organisationId = organisation.getId();
        final var tournament = buildTournament();
        final var tournamentId = organisation.getTournamentId();
        final var tournamentUuid = organisation.getTournamentId().value();
        final var team = buildTeam(organisationId, tournamentId);
        final var teamId = team.getId();
        final var request = buildUpdateRequest();
        final var responsible = request.getResponsible();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findById(teamId)).thenReturn(Optional.of(team));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamUpdated = teamService.updateTeam(tournamentUuid, teamId.value(), request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(organisationService).updateResponsible(responsible, organisationId);
        verify(teamStore).findById(teamId);
        verify(teamStore).save(any(Team.class));

        assertEquals(request.getName(), teamUpdated.getName());
    }

    @Test
    void updateTeamWhenTeamNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId().value();
        final var teamId = buildTeam().getId();
        final var request = buildUpdateRequest();

        when(tournamentSearchService.findById(tournamentId)).thenReturn(tournament);
        when(teamStore.findById(teamId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> teamService.updateTeam(tournamentId, teamId.value(), request));

        verify(tournamentSearchService).findById(tournamentId);
        verify(teamStore).findById(teamId);

        assertTrue(exception.getMessage().contains("Team"));
        assertTrue(exception.getMessage().contains(teamId.value().toString()));
    }

    @Test
    void updateTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentId = tournament.getId().value();
        final var teamId = buildTeam().getId().value();
        final var request = buildUpdateRequest();

        when(tournamentSearchService.findById(tournamentId)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> teamService.updateTeam(tournamentId, teamId, request));

        verify(tournamentSearchService).findById(tournamentId);

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation", exception.getMessage());
    }

    @Test
    void deleteTeamWhenLastInOrgShouldDeleteOrg() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation().getId();
        final var team = buildTeam(organisationId, tournamentId);
        final var teamId = team.getId();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findById(teamId)).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(organisationId)).thenReturn(0L);

        teamService.deleteTeam(tournamentUuid, teamId.value());

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(teamStore).findById(teamId);
        verify(teamStore).deleteById(teamId);
        verify(teamStore).countByOrganisationId(organisationId);
        verify(organisationStore).deleteById(organisationId);
    }

    @Test
    void deleteTeamWhenNotLastInOrgShouldNotDeleteOrg() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation().getId();
        final var team = buildTeam(organisationId, tournamentId);
        final var teamId = team.getId();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findById(teamId)).thenReturn(Optional.of(team));
        when(teamStore.countByOrganisationId(organisationId)).thenReturn(1L);

        teamService.deleteTeam(tournamentUuid, teamId.value());

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(teamStore).findById(teamId);
        verify(teamStore).deleteById(teamId);
        verify(teamStore).countByOrganisationId(organisationId);
        verify(organisationStore, never()).deleteById(any());
    }

    @Test
    void deleteTeamWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentId = tournament.getId().value();
        final var teamId = buildTeam().getId().value();

        when(tournamentSearchService.findById(tournamentId)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> teamService.deleteTeam(tournamentId, teamId));

        verify(tournamentSearchService).findById(tournamentId);

        assertEquals("Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation", exception.getMessage());
    }

    @Test
    void markPaidWhenExistsShouldUpdatePaidStatus() {
        final var organisation = buildOrganisation();
        final var organisationId = organisation.getId();
        final var tournamentId = organisation.getTournamentId();
        final var team = buildTeam(organisationId, tournamentId);
        final var teamId = team.getId();

        when(teamStore.findById(teamId)).thenReturn(Optional.of(team));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());

        final var teamUpdated = teamService.markPaid(teamId.value(), true);

        verify(teamStore).findById(teamId);
        verify(teamStore).save(any(Team.class));

        assertTrue(teamUpdated.isPaid());
    }

    @Test
    void markPaidWhenNotFoundShouldThrowNotFoundException() {
        final var teamId = buildTeam().getId();

        when(teamStore.findById(teamId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> teamService.markPaid(teamId.value(), true));

        verify(teamStore).findById(teamId);

        assertTrue(exception.getMessage().contains("Team"));
    }
}
