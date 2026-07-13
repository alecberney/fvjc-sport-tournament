package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.fakes.TeamFakes;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamSearchService;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildSwapRequest;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private TeamSearchService teamSearchService;

    @Mock
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private GroupService groupService;

    @Test
    void generateWhenValidShouldCreateOneGroupForFourTeams() {
        final var tournament = buildTournament();
        final var org1 = OrganisationId.of(UUID.randomUUID());
        final var org2 = OrganisationId.of(UUID.randomUUID());
        final var teams = List.of(
            TeamFakes.buildTeam(org1, tournament.getId()),
            TeamFakes.buildTeam(org1, tournament.getId()),
            TeamFakes.buildTeam(org2, tournament.getId()),
            TeamFakes.buildTeam(org2, tournament.getId())
        );
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(teams);

        final var groupsGenerated = groupService.generate(tournament.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(teamStore).findAllByTournamentId(tournament.getId());
        verify(groupStore).deleteAllByTournamentId(tournament.getId());
        verify(groupStore).saveAll(anyList());
        verify(teamStore, times(8)).save(any(Team.class));

        assertEquals(1, groupsGenerated.size());
        assertEquals("A", groupsGenerated.get(0).getName());
        assertNotNull(groupsGenerated.get(0).getId());
    }

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> groupService.generate(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les groupes ne peuvent être générés que pour un tournoi en cours de préparation",
            exception.getMessage());
    }

    @Test
    void generateWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournament.getId()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("groupSize", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenNotEnoughTeamsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournament.getId()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("groupSize", exception.getErrors().get(0).field());
    }

    @Test
    void distributionWhenValidShouldReturnCorrectCounts() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var teams = List.of(
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId()),
            TeamFakes.buildTeam(org, tournament.getId())
        );
        final var request = GroupGenerateRequest.builder().groupSize(2).build();

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var distribution = groupService.distribution(tournamentId, request);

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals(2, distribution.getNumberOfGroups());
        assertEquals(1, distribution.getGroupsOfBaseSizePlusOne());
        assertEquals(1, distribution.getGroupsOfBaseSize());
        assertEquals(2, distribution.getBaseSize());
        assertEquals(5, distribution.getTotalTeams());
    }

    @Test
    void distributionWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.distribution(tournamentId, request));

        verify(teamStore).findAllByTournamentId(tournamentId);

        assertEquals("groupSize", exception.getErrors().get(0).field());
    }

    @Test
    void findAllByTournamentIdShouldReturnGroupViewsWithTeams() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = buildGroup(tournament.getId());
        final var groupId = group.getId();
        final var team = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId)).thenReturn(List.of(team));

        final var groupsFound = groupService.findAllByTournamentId(tournament.getId());

        verify(groupStore).findAllByTournamentId(tournament.getId());
        verify(teamStore).findAllByGroupId(groupId);

        assertEquals(1, groupsFound.size());
        assertEquals("A", groupsFound.get(0).getName());
        assertEquals(1, groupsFound.get(0).getTeams().size());
    }

    @Test
    void swapWhenValidShouldExchangeTeamGroups() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group1 = buildGroup(tournament.getId()).withName("A");
        final var group2 = buildGroup(tournament.getId()).withName("B");
        final var groupId1 = group1.getId();
        final var groupId2 = group2.getId();
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group1, group2));
        when(teamSearchService.findById(team1.getId())).thenReturn(team1);
        when(teamSearchService.findById(team2.getId())).thenReturn(team2);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(groupId1)).thenReturn(List.of(team2));
        when(teamStore.findAllByGroupId(groupId2)).thenReturn(List.of(team1));

        final var groupsUpdated = groupService.swap(tournament.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(teamStore, times(2)).save(any(Team.class));

        assertEquals(2, groupsUpdated.size());
        final var groupAView = groupsUpdated.stream().filter(g -> g.getName().equals("A")).findFirst().orElseThrow();
        final var groupBView = groupsUpdated.stream().filter(g -> g.getName().equals("B")).findFirst().orElseThrow();
        assertEquals(1, groupAView.getTeams().size());
        assertEquals(team2.getId(), groupAView.getTeams().get(0).getId());
        assertEquals(1, groupBView.getTeams().size());
        assertEquals(team1.getId(), groupBView.getTeams().get(0).getId());
    }

    @Test
    void swapWhenSameGroupShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = buildGroup(tournament.getId());
        final var groupId = group.getId();
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(teamSearchService.findById(team1.getId())).thenReturn(team1);
        when(teamSearchService.findById(team2.getId())).thenReturn(team2);

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.swap(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("teamId2", exception.getErrors().get(0).field());
    }

    @Test
    void swapWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildSwapRequest(UUID.randomUUID(), UUID.randomUUID());

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> groupService.swap(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Les groupes ne peuvent être générés que pour un tournoi en cours de préparation",
            exception.getMessage());
    }

    @Test
    void distributionWhenRemainderExceedsGroupCountShouldComputeCorrectly() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var teams = new ArrayList<Team>();
        for (int i = 0; i < 11; i++) {
            teams.add(TeamFakes.buildTeam(org, tournament.getId()));
        }
        final var request = GroupGenerateRequest.builder().groupSize(4).build();

        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var distribution = groupService.distribution(tournamentId, request);

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
        final var org = OrganisationId.of(UUID.randomUUID());
        final var teams = new ArrayList<Team>();
        for (int i = 0; i < 11; i++) {
            teams.add(TeamFakes.buildTeam(org, tournament.getId()));
        }
        final var request = GroupGenerateRequest.builder().groupSize(4).build();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(new ArrayList<>());

        final var groupsGenerated = groupService.generate(tournament.getId(), request);

        verify(tournamentSearchService).findById(tournament.getId());
        verify(groupStore).saveAll(anyList());

        assertEquals(3, groupsGenerated.size());
    }

    @Test
    void deleteAllByTournamentIdShouldDeleteGroups() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());

        groupService.deleteAllByTournamentId(tournamentId);

        verify(groupStore).deleteAllByTournamentId(tournamentId);
    }
}
