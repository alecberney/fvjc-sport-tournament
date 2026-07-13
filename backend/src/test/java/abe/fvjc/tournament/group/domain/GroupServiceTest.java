package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamFakes;
import abe.fvjc.tournament.team.domain.TeamStore;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.group.domain.GroupFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

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

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(teams);

        final var groupsGenerated = groupService.generate(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore).findAllByTournamentId(tournament.getId().value());
        verify(groupStore).deleteAllByTournamentId(tournament.getId().value());
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

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void generateWhenGroupSizeLessThan2ShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        final var exception = assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());

        assertEquals("groupSize", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenNotEnoughTeamsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value()))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        assertThrows(ValidationException.class,
            () -> groupService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void distributionWhenValidShouldReturnCorrectCounts() {
        final var tournamentId = UUID.randomUUID();
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
        final var tournamentId = UUID.randomUUID();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        final var request = GroupGenerateRequest.builder().groupSize(1).build();

        when(teamStore.findAllByTournamentId(tournamentId))
            .thenReturn(List.of(TeamFakes.buildTeam(org, tournament.getId())));

        assertThrows(ValidationException.class,
            () -> groupService.distribution(tournamentId, request));

        verify(teamStore).findAllByTournamentId(tournamentId);
    }

    @Test
    void findAllByTournamentIdShouldReturnGroupViewsWithTeams() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = IdGenerator.groupId();
        final var group = buildGroup(tournament.getId()).withId(groupId);
        final var team = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);

        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByGroupId(groupId.value())).thenReturn(List.of(team));

        final var groupsFound = groupService.findAllByTournamentId(tournament.getId().value());

        verify(groupStore).findAllByTournamentId(tournament.getId().value());
        verify(teamStore).findAllByGroupId(groupId.value());

        assertEquals(1, groupsFound.size());
        assertEquals("A", groupsFound.get(0).getName());
        assertEquals(1, groupsFound.get(0).getTeams().size());
    }

    @Test
    void swapWhenValidShouldExchangeTeamGroups() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = IdGenerator.groupId();
        final var groupId2 = IdGenerator.groupId();
        final var group1 = buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = buildGroup(tournament.getId()).withId(groupId2).withName("B");
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(teamStore.findById(team1.getId().value())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId().value())).thenReturn(Optional.of(team2));
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(groupId1.value())).thenReturn(List.of(team2));
        when(teamStore.findAllByGroupId(groupId2.value())).thenReturn(List.of(team1));

        final var groupsUpdated = groupService.swap(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(teamStore, times(2)).save(any(Team.class));

        assertEquals(2, groupsUpdated.size());
        // verify the groups contain the swapped teams
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
        final var groupId = IdGenerator.groupId();
        final var group = buildGroup(tournament.getId()).withId(groupId);
        final var team1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var team2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var request = buildSwapRequest(team1.getId().value(), team2.getId().value());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findById(team1.getId().value())).thenReturn(Optional.of(team1));
        when(teamStore.findById(team2.getId().value())).thenReturn(Optional.of(team2));

        assertThrows(ValidationException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void swapWhenTeamNotFoundShouldThrowNotFoundException() {
        final var tournament = buildTournament();
        final var teamId1 = UUID.randomUUID();
        final var request = buildSwapRequest(teamId1, UUID.randomUUID());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findById(teamId1)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void swapWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildSwapRequest(UUID.randomUUID(), UUID.randomUUID());

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> groupService.swap(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void distributionWhenRemainderExceedsGroupCountShouldComputeCorrectly() {
        final var tournamentId = UUID.randomUUID();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var tournament = buildTournament();
        // 11 teams, groupSize 4 → remainder(3) > numberOfGroups(2) → needs 3 groups
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

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(teams);
        when(teamStore.save(any(Team.class))).then(returnsFirstArg());
        when(teamStore.findAllByGroupId(any())).thenReturn(new ArrayList<>());

        final var groupsGenerated = groupService.generate(tournament.getId().value(), request);

        verify(tournamentStore).findById(tournament.getId().value());
        verify(groupStore).saveAll(anyList());

        assertEquals(3, groupsGenerated.size());
    }

    @Test
    void deleteAllByTournamentIdShouldDeleteGroups() {
        final var tournamentId = UUID.randomUUID();

        groupService.deleteAllByTournamentId(tournamentId);

        verify(groupStore).deleteAllByTournamentId(tournamentId);
    }
}
