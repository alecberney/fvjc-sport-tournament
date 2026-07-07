package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildSwapRequest;
import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock
    private GroupStore groupStore;
    @Mock
    private TeamStore teamStore;
    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private GroupService groupService;

    @Test
    void generateWhenValidShouldCreateOneGroupForFourTeams() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId1 = buildOrganisation(tournamentId).getId();
        final var organisationId2 = buildOrganisation(tournamentId).getId();
        final var teams = List.of(
            buildTeam(organisationId1, tournamentId),
            buildTeam(organisationId1, tournamentId),
            buildTeam(organisationId2, tournamentId),
            buildTeam(organisationId2, tournamentId));
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(teams);

        final var groupsGenerated = groupService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(teamStore).findAllByTournamentId(tournamentId);
        verify(groupStore).deleteAllByTournamentId(tournamentId);
        verify(groupStore).saveAll(anyList());
        verify(teamStore, times(8)).save(any(Team.class));

        assertEquals(1, groupsGenerated.size());
        assertEquals("A", groupsGenerated.getFirst().getName());
        assertNotNull(groupsGenerated.getFirst().getId());
    }

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentUuid = tournament.getId().value();
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> groupService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("Les groupes ne peuvent être générés que pour un tournoi en cours de préparation", exception.getMessage());
    }

    @Test
    void generateWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(buildTeam(organisationId, tournamentId)));

        final var exception = assertThrows(
                ValidationException.class,
                () -> groupService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("groupSize", exception.getErrors().getFirst().field());
    }

    @Test
    void generateWhenNotEnoughTeamsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(buildTeam(organisationId, tournamentId)));

        final var exception = assertThrows(
                ValidationException.class,
                () -> groupService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("groupSize", exception.getErrors().getFirst().field());
    }

    @Test
    void distributionWhenValidShouldReturnCorrectCounts() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var teams = List.of(
            buildTeam(organisationId, tournamentId),
            buildTeam(organisationId, tournamentId),
            buildTeam(organisationId, tournamentId),
            buildTeam(organisationId, tournamentId),
            buildTeam(organisationId, tournamentId));
        final var request = GroupGenerateRequest.builder().groupSize(2).build();

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var distribution = groupService.distribution(tournamentUuid, request);

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals(2, distribution.getNumberOfGroups());
        assertEquals(1, distribution.getGroupsOfBaseSizePlusOne());
        assertEquals(1, distribution.getGroupsOfBaseSize());
        assertEquals(2, distribution.getBaseSize());
        assertEquals(5, distribution.getTotalTeams());
    }

    @Test
    void distributionWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(buildTeam(organisationId, tournamentId)));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.distribution(tournamentUuid, request));

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals("groupSize", exception.getErrors().getFirst().field());
    }

    @Test
    void findAllByTournamentIdShouldReturnGroupViewsWithTeams() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var group = buildGroup(tournamentId);
        final var groupId = group.getId();
        final var team = buildTeam(organisationId, tournamentId, groupId);

        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team));

        final var groupsFound = groupService.findAllByTournamentId(tournamentUuid);

        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(teamStore).findAllByGroupId(groupId);

        assertEquals(1, groupsFound.size());
        assertEquals("A", groupsFound.getFirst().getName());
        assertEquals(1, groupsFound.getFirst().getTeams().size());
    }

    @Test
    void swapWhenValidShouldExchangeTeamGroups() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var group1 = buildGroup(tournamentId);
        final var groupId1 = group1.getId();
        final var group2 = buildGroup(tournamentId)
                .withName("B");
        final var groupId2 = group2.getId();
        final var team1 = buildTeam(organisationId, tournamentId).withGroupId(groupId1);
        final var team2 = buildTeam(organisationId, tournamentId).withGroupId(groupId2);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group1, group2));
        when(teamStore.findById(team1.getId())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId())).thenReturn(Optional.of(team2));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(groupId1)).thenReturn(List.of(team2));
        when(teamStore.findAllByGroupId(groupId2)).thenReturn(List.of(team1));

        final var groupsUpdated = groupService.swap(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(teamStore, times(2)).save(any(Team.class));

        assertEquals(2, groupsUpdated.size());
        // verify the groups contain the swapped teams
        final var groupAView = groupsUpdated.stream().filter(g -> g.getName().equals("A")).findFirst().orElseThrow();
        final var groupBView = groupsUpdated.stream().filter(g -> g.getName().equals("B")).findFirst().orElseThrow();
        assertEquals(1, groupAView.getTeams().size());
        assertEquals(team2.getId(), groupAView.getTeams().getFirst().getId());
        assertEquals(1, groupBView.getTeams().size());
        assertEquals(team1.getId(), groupBView.getTeams().getFirst().getId());
    }

    @Test
    void swapWhenSameGroupShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var group = buildGroup(tournamentId);
        final var groupId = group.getId();
        final var team1 = buildTeam(organisationId, tournamentId, groupId);
        final var team2 = buildTeam(organisationId, tournamentId, groupId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findById(team1.getId())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId())).thenReturn(Optional.of(team2));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.swap(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("teamId2", exception.getErrors().getFirst().field());
    }

    @Test
    void swapWhenTeamNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var team1 = buildTeam(organisationId, tournamentId);
        final var team2 = buildTeam(organisationId, tournamentId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());
        when(teamStore.findById(team1.getId())).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class,
            () -> groupService.swap(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertTrue(exception.getMessage().contains("Team"));
    }

    @Test
    void swapWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var team1 = buildTeam(organisationId, tournamentId);
        final var team2 = buildTeam(organisationId, tournamentId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> groupService.swap(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);

        assertEquals("Les groupes ne peuvent être générés que pour un tournoi en cours de préparation", exception.getMessage());
    }

    @Test
    void distributionWhenRemainderExceedsGroupCountShouldComputeCorrectly() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        // 11 teams, groupSize 4 → remainder(3) > numberOfGroups(2) → needs 3 groups
        final var teams = new ArrayList<Team>();
        for (int i = 0; i < 11; i++) {
            teams.add(buildTeam(organisationId, tournamentId));
        }
        final var request = GroupGenerateRequest.builder().groupSize(4).build();

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var distribution = groupService.distribution(tournamentUuid, request);

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals(11, distribution.getTotalTeams());
        assertEquals(3, distribution.getNumberOfGroups());
        assertEquals(distribution.getGroupsOfBaseSize() + distribution.getGroupsOfBaseSizePlusOne(), 3);
        assertEquals(distribution.getGroupsOfBaseSize() * distribution.getBaseSize()
                + distribution.getGroupsOfBaseSizePlusOne() * (distribution.getBaseSize() + 1), 11);
    }

    @Test
    void generateWhenRemainderExceedsGroupCountShouldPlaceAllTeams() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation(tournamentId).getId();
        final var teams = new ArrayList<Team>();
        for (int i = 0; i < 11; i++) {
            teams.add(buildTeam(organisationId, tournamentId));
        }
        final var request = GroupGenerateRequest.builder().groupSize(4).build();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(new ArrayList<>());

        final var groupsGenerated = groupService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).saveAll(anyList());

        assertEquals(3, groupsGenerated.size());
    }
}
