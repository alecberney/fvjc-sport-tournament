package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.Group;
import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.schedule.domain.ScheduleFakes.*;
import static abe.fvjc.tournament.tournament.domain.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.tournament.domain.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private GroupStore groupStore;

    @Mock
    private MatchStore matchStore;

    @Mock
    private RoundStore roundStore;

    @Mock
    private TeamStore teamStore;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void generateWhenTournamentNotFoundShouldThrowNotFoundException() {
        final var tournamentId = UUID.randomUUID();
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
            () -> scheduleService.generate(tournamentId, request));

        verify(tournamentStore).findById(tournamentId);
    }

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));

        assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
    }

    @Test
    void generateWhenNoGroupsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());

        final var exception = assertThrows(ValidationException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(tournamentStore).findById(tournament.getId().value());
        verify(groupStore).findAllByTournamentId(tournament.getId().value());

        assertEquals("groups", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenResultsExistShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var group = GroupFakes.buildGroup(tournament.getId());
        final var existingRound = buildRound(tournament.getId());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(existingRound));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(true);

        assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId().value(), request));

        verify(matchStore).existsResultByRoundIds(anyList());
    }

    @Test
    void generateWhenValidShouldReturnScheduleOverview() {
        final var tournament = buildTournament(); // 4 fields
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        // 3 teams per group → 3 round-robin matches each; 2 groups on 2 different fields → 3 rounds, 6 matches
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t5 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t6 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2, t3, t4, t5, t6));

        final var scheduleGenerated = scheduleService.generate(tournament.getId().value(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
        verify(tournamentStore, never()).save(any());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(6, scheduleGenerated.getTotalMatches());
        assertEquals(3, scheduleGenerated.getRounds().size());
        assertEquals(2, scheduleGenerated.getRounds().get(0).getMatches().size());
        assertEquals(1, scheduleGenerated.getRounds().get(0).getNumber());
    }

    @Test
    void generateWhenTwoGroupsOnSameFieldShouldComputeTotalRoundsFromLongestQueue() {
        final var tournament = buildTournament().withNumberOfFields(1);
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        // Group A: 2 teams → 1 round (1 match)
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        // Group B: 4 teams → 3 rounds (6 matches)
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        final var tA1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tA2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tB1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(tA1, tA2, tB1, tB2, tB3, tB4));

        final var scheduleGenerated = scheduleService.generate(tournament.getId().value(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        // Field 1 queue: [A0, B0, B1, B2, B3, B4, B5] → 7 slots → 7 rounds, 7 matches
        assertEquals(7, scheduleGenerated.getTotalRounds());
        assertEquals(7, scheduleGenerated.getTotalMatches());
        assertEquals(7, scheduleGenerated.getRounds().size());
        assertTrue(scheduleGenerated.getRounds().get(0).getMatches().size() >= 1);
    }

    @Test
    void generateWhenExistingRoundsShouldDeleteBeforePersisting() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId);
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var existingRound = buildRound(tournament.getId());
        final var request = buildGenerateRequest();

        when(tournamentStore.findById(tournament.getId().value())).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(false);
        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(existingRound));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2, t3));

        scheduleService.generate(tournament.getId().value(), request);

        verify(matchStore).deleteAllByRoundIds(anyList());
        verify(roundStore).deleteAllByTournamentId(tournament.getId().value());
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
    }

    @Test
    void findByTournamentIdWhenNoRoundsShouldReturnEmptyOverview() {
        final var tournamentId = UUID.randomUUID();

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        final var scheduleFound = scheduleService.findByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);

        assertEquals(0, scheduleFound.getTotalRounds());
        assertEquals(0, scheduleFound.getTotalMatches());
        assertTrue(scheduleFound.getRounds().isEmpty());
    }

    @Test
    void findByTournamentIdWhenRoundsExistShouldReturnPopulatedOverview() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId).withName("A");
        final var round = buildRound(tournament.getId());
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId());

        when(roundStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(round));
        when(matchStore.findAllByRoundIds(anyList())).thenReturn(List.of(match));
        when(groupStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(group));
        when(teamStore.findAllByTournamentId(tournament.getId().value())).thenReturn(List.of(t1, t2));

        final var scheduleFound = scheduleService.findByTournamentId(tournament.getId().value());

        verify(roundStore).findAllByTournamentId(tournament.getId().value());
        verify(matchStore).findAllByRoundIds(anyList());

        assertEquals(1, scheduleFound.getTotalRounds());
        assertEquals(1, scheduleFound.getTotalMatches());
        assertEquals(1, scheduleFound.getRounds().size());
        assertEquals("A", scheduleFound.getRounds().get(0).getMatches().get(0).getGroupName());
        assertEquals(t1.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam1().getName());
        assertEquals(t2.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam2().getName());
    }
}
