package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.fakes.GroupFakes;
import abe.fvjc.tournament.domain.fakes.ScheduleFakes;
import abe.fvjc.tournament.domain.fakes.TeamFakes;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
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
import java.util.stream.Stream;

import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private TournamentSearchService tournamentSearchService;

    @InjectMocks
    private ScheduleService scheduleService;

    @Test
    void generateWhenTournamentNotDraftShouldThrowConflictException() {
        final var tournament = buildTournament().withStatus(IN_PROGRESS);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);

        final var exception = assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());

        assertEquals("Le calendrier ne peut être généré que pour un tournoi en préparation",
            exception.getMessage());
    }

    @Test
    void generateWhenNoGroupsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());

        final var exception = assertThrows(ValidationException.class,
            () -> scheduleService.generate(tournament.getId(), request));

        verify(tournamentSearchService).findById(tournament.getId());
        verify(groupStore).findAllByTournamentId(tournament.getId());

        assertEquals("groups", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenResultsExistShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var group = GroupFakes.buildGroup(tournament.getId());
        final var existingRound = buildRound(tournament.getId());
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(existingRound));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(true);

        final var exception = assertThrows(ConflictException.class,
            () -> scheduleService.generate(tournament.getId(), request));

        verify(matchStore).existsResultByRoundIds(anyList());

        assertEquals("Impossible de régénérer le calendrier : des résultats ont déjà été saisis",
            exception.getMessage());
    }

    @Test
    void generateWhenValidShouldReturnScheduleOverview() {
        final var tournament = buildTournament(); // 4 fields
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        // 3 teams per group → each group is a triangle: at most 1 match per group per round.
        // With 2 groups that is 2 matches per round → 3 rounds, 6 matches (fields to spare).
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var t4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t5 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var t6 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(t1, t2, t3, t4, t5, t6));

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(6, scheduleGenerated.getTotalMatches());
        assertEquals(3, scheduleGenerated.getRounds().size());
        assertEquals(2, scheduleGenerated.getRounds().get(0).getMatches().size());
        assertEquals(1, scheduleGenerated.getRounds().get(0).getNumber());
    }

    @Test
    void generateWhenSingleFieldShouldScheduleOneMatchPerRound() {
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

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(tA1, tA2, tB1, tB2, tB3, tB4));

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        // Only 1 field → at most 1 match per round → 7 matches spread over 7 rounds
        assertEquals(7, scheduleGenerated.getTotalRounds());
        assertEquals(7, scheduleGenerated.getTotalMatches());
        assertEquals(7, scheduleGenerated.getRounds().size());
        assertTrue(scheduleGenerated.getRounds().get(0).getMatches().size() >= 1);
    }

    @Test
    void generateWhenSingleFieldShouldScheduleIdleGroupEarlyForRestFairness() {
        final var tournament = buildTournament().withNumberOfFields(1);
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        final var tA1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tA2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1);
        final var tB1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var tB4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(tA1, tA2, tB1, tB2, tB3, tB4));

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        final var groupARoundNumber = scheduleGenerated.getRounds().stream()
                .filter(round -> round.getMatches().stream()
                        .anyMatch(match -> match.getGroupName().equals("A")))
                .map(RoundOverview::getNumber)
                .findFirst()
                .orElseThrow();

        assertEquals(7, scheduleGenerated.getTotalRounds());
        assertTrue(groupARoundNumber <= 3,
                "Group A should be scheduled early for rest fairness but was round " + groupARoundNumber);
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

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(false);
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(existingRound));
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(t1, t2, t3));

        scheduleService.generate(tournament.getId(), request);

        verify(matchStore).deleteAllByRoundIds(anyList());
        verify(roundStore).deleteAllByTournamentId(tournament.getId());
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
    }

    @Test
    void generateWhenGroupLargerThanFieldsShouldPackSameGroupMatchesInOneRound() {
        final var tournament = buildTournament(); // 4 fields
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId = GroupId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withId(groupId).withName("A");
        // 4 teams → 6 round-robin matches, each team plays 3 → minimum 3 rounds,
        // two disjoint matches of the SAME group can share a round → 2 matches per round.
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var t4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(t1, t2, t3, t4));

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(6, scheduleGenerated.getTotalMatches());
        scheduleGenerated.getRounds().forEach(round -> assertEquals(2, round.getMatches().size()));
    }

    @Test
    void generateShouldFillAllFieldsAndNeverScheduleTeamTwiceInSameRound() {
        final var tournament = buildTournament(); // 4 fields
        final var org = OrganisationId.of(UUID.randomUUID());
        final var groupId1 = GroupId.of(UUID.randomUUID());
        final var groupId2 = GroupId.of(UUID.randomUUID());
        final var group1 = GroupFakes.buildGroup(tournament.getId()).withId(groupId1).withName("A");
        final var group2 = GroupFakes.buildGroup(tournament.getId()).withId(groupId2).withName("B");
        // Two groups of 4 teams → 12 matches; each group contributes 2 disjoint matches per round,
        // filling all 4 fields → 3 rounds of 4 matches.
        final var teams = new ArrayList<Team>();
        for (int i = 0; i < 4; i++) {
            teams.add(TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId1));
        }
        for (int i = 0; i < 4; i++) {
            teams.add(TeamFakes.buildTeam(org, tournament.getId()).withGroupId(groupId2));
        }
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group1, group2));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(teams);

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(12, scheduleGenerated.getTotalMatches());
        scheduleGenerated.getRounds().forEach(round -> {
            assertEquals(4, round.getMatches().size());

            final var teamIds = round.getMatches().stream()
                    .flatMap(m -> Stream.of(m.getTeam1().getId(), m.getTeam2().getId()))
                    .toList();
            final var distinctTeamIds = teamIds.stream().distinct().toList();
            final var distinctFields = round.getMatches().stream()
                    .map(MatchOverview::getField)
                    .distinct()
                    .toList();

            assertEquals(teamIds.size(), distinctTeamIds.size());
            assertEquals(round.getMatches().size(), distinctFields.size());
        });
    }

    @Test
    void deleteAllByTournamentIdWhenRoundsExistShouldDeleteMatchesThenRounds() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());
        final var round = ScheduleFakes.buildRound(tournamentId);

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(round));

        scheduleService.deleteAllByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore).deleteAllByRoundIds(List.of(round.getId()));
        verify(roundStore).deleteAllByTournamentId(tournamentId);
    }

    @Test
    void deleteAllByTournamentIdWhenNoRoundsShouldNotDeleteAnything() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        scheduleService.deleteAllByTournamentId(tournamentId);

        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore, never()).deleteAllByRoundIds(anyList());
        verify(roundStore, never()).deleteAllByTournamentId(tournamentId);
    }

    @Test
    void findByTournamentIdWhenNoRoundsShouldReturnEmptyOverview() {
        final var tournamentId = TournamentId.of(UUID.randomUUID());

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

        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(round));
        when(matchStore.findAllByRoundIds(anyList())).thenReturn(List.of(match));
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(t1, t2));

        final var scheduleFound = scheduleService.findByTournamentId(tournament.getId());

        verify(roundStore).findAllByTournamentId(tournament.getId());
        verify(matchStore).findAllByRoundIds(anyList());

        assertEquals(1, scheduleFound.getTotalRounds());
        assertEquals(1, scheduleFound.getTotalMatches());
        assertEquals(1, scheduleFound.getRounds().size());
        assertEquals("A", scheduleFound.getRounds().get(0).getMatches().get(0).getGroupName());
        assertEquals(t1.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam1().getName());
        assertEquals(t2.getName(), scheduleFound.getRounds().get(0).getMatches().get(0).getTeam2().getName());
    }
}
