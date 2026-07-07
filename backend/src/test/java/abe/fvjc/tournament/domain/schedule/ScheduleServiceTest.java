package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static abe.fvjc.tournament.domain.fakes.MatchFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.fakes.GroupFakes.buildGroup;
import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
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
        final var tournament = buildTournament()
                .withStatus(TournamentStatus.IN_PROGRESS);
        final var tournamentId = tournament.getId().value();
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentId)).thenReturn(tournament);

        final var exception = assertThrows(
                ConflictException.class,
                () -> scheduleService.generate(tournamentId, request));

        verify(tournamentSearchService).findById(tournamentId);

        assertEquals("Le calendrier ne peut être généré que pour un tournoi en préparation", exception.getMessage());
    }

    @Test
    void generateWhenNoGroupsShouldThrowValidationException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        final var exception = assertThrows(
                ValidationException.class,
                () -> scheduleService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);

        assertEquals("groups", exception.getErrors().getFirst().field());
    }

    @Test
    void generateWhenResultsExistShouldThrowConflictException() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var group = buildGroup(tournamentId);
        final var existingRound = buildRound(tournamentId);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(existingRound));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(true);

        final var exception = assertThrows(
                ConflictException.class,
                () -> scheduleService.generate(tournamentUuid, request));

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore).existsResultByRoundIds(anyList());

        assertEquals("Impossible de régénérer le calendrier : des résultats ont déjà été saisis", exception.getMessage());
    }

    @Test
    void generateWhenValidShouldReturnScheduleOverview() {
        final var tournament = buildTournament(); // 4 fields
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation().getId();
        final var group1 = buildGroup(tournamentId);
        final var groupId1 = group1.getId();
        final var group2 = buildGroup(tournamentId)
                .withName("B");
        final var groupId2 = group2.getId();
        final var groups = List.of(group1, group2);
        // 3 teams per group → 3 round-robin matches each; 2 groups on 2 different fields → 3 rounds, 6 matches
        final var t1 = buildTeam(organisationId, tournamentId, groupId1);
        final var t2 = buildTeam(organisationId, tournamentId, groupId1);
        final var t3 = buildTeam(organisationId, tournamentId, groupId1);
        final var t4 = buildTeam(organisationId, tournamentId, groupId2);
        final var t5 = buildTeam(organisationId, tournamentId, groupId2);
        final var t6 = buildTeam(organisationId, tournamentId, groupId2);
        final var teams = List.of(t1, t2, t3, t4, t5, t6);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(groups);
        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var scheduleGenerated = scheduleService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(3, scheduleGenerated.getTotalRounds());
        assertEquals(6, scheduleGenerated.getTotalMatches());
        assertEquals(3, scheduleGenerated.getRounds().size());
        assertEquals(2, scheduleGenerated.getRounds().getFirst().getMatches().size());
        assertEquals(1, scheduleGenerated.getRounds().getFirst().getNumber());
    }

    @Test
    void generateWhenTwoGroupsOnSameFieldShouldComputeTotalRoundsFromLongestQueue() {
        final var tournament = buildTournament()
                .withNumberOfFields(1);
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation().getId();
        // Group A: 2 teams → 1 round (1 match)
        final var group1 = buildGroup(tournamentId);
        final var groupId1 = group1.getId();
        // Group B: 4 teams → 3 rounds (6 matches)
        final var group2 = buildGroup(tournamentId).withName("B");
        final var groupId2 = group2.getId();
        final var groups = List.of(group1, group2);
        final var tA1 = buildTeam(organisationId, tournamentId, groupId1);
        final var tA2 = buildTeam(organisationId, tournamentId, groupId1);
        final var tB1 = buildTeam(organisationId, tournamentId, groupId2);
        final var tB2 = buildTeam(organisationId, tournamentId, groupId2);
        final var tB3 = buildTeam(organisationId, tournamentId, groupId2);
        final var tB4 = buildTeam(organisationId, tournamentId, groupId2);
        final var teams = List.of(tA1, tA2, tB1, tB2, tB3, tB4);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(groups);
        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        final var scheduleGenerated = scheduleService.generate(tournamentUuid, request);

        verify(tournamentSearchService).findById(tournamentUuid);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        // Field 1 queue: [A0, B0, B1, B2, B3, B4, B5] → 7 slots → 7 rounds, 7 matches
        assertEquals(7, scheduleGenerated.getTotalRounds());
        assertEquals(7, scheduleGenerated.getTotalMatches());
        assertEquals(7, scheduleGenerated.getRounds().size());
        assertFalse(scheduleGenerated.getRounds().getFirst().getMatches().isEmpty());
    }

    @Test
    void generateWhenExistingRoundsShouldDeleteBeforePersisting() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var tournamentUuid = tournamentId.value();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(tournamentId);
        final var groupId = group.getId();
        final var t1 = buildTeam(organisationId, tournamentId, groupId);
        final var t2 = buildTeam(organisationId, tournamentId, groupId);
        final var t3 = buildTeam(organisationId, tournamentId, groupId);
        final var teams = List.of(t1, t2, t3);
        final var existingRound = buildRound(tournamentId);
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournamentUuid)).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(matchStore.existsResultByRoundIds(anyList())).thenReturn(false);
        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(existingRound));
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(teams);

        scheduleService.generate(tournamentUuid, request);

        verify(matchStore).deleteAllByRoundIds(anyList());
        verify(roundStore).deleteAllByTournamentId(tournamentId);
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
    }

    @Test
    void findByTournamentIdWhenNoRoundsShouldReturnEmptyOverview() {
        final var tournamentId = buildTournament().getId();

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of());

        final var scheduleFound = scheduleService.findByTournamentId(tournamentId.value());

        verify(roundStore).findAllByTournamentId(tournamentId);

        assertEquals(0, scheduleFound.getTotalRounds());
        assertEquals(0, scheduleFound.getTotalMatches());
        assertTrue(scheduleFound.getRounds().isEmpty());
    }

    @Test
    void findByTournamentIdWhenRoundsExistShouldReturnPopulatedOverview() {
        final var tournament = buildTournament();
        final var tournamentId = tournament.getId();
        final var organisationId = buildOrganisation().getId();
        final var group = buildGroup(tournamentId);
        final var groupId = group.getId();
        final var round = buildRound(tournamentId);
        final var t1 = buildTeam(organisationId, tournamentId, groupId);
        final var t2 = buildTeam(organisationId, tournamentId, groupId);
        final var match = buildMatch(round.getId())
                .withGroupId(groupId)
                .withTeam1Id(t1.getId())
                .withTeam2Id(t2.getId());

        when(roundStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(round));
        when(matchStore.findAllByRoundIds(anyList())).thenReturn(List.of(match));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(group));
        when(teamStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(t1, t2));

        final var scheduleFound = scheduleService.findByTournamentId(tournamentId.value());

        verify(roundStore).findAllByTournamentId(tournamentId);
        verify(matchStore).findAllByRoundIds(anyList());

        assertEquals(1, scheduleFound.getTotalRounds());
        assertEquals(1, scheduleFound.getTotalMatches());
        assertEquals(1, scheduleFound.getRounds().size());
        final var firstMatch = scheduleFound.getRounds().getFirst().getMatches().getFirst();
        assertEquals("A", firstMatch.getGroup().getName());
        assertEquals(t1.getName(), firstMatch.getTeam1().getName());
        assertEquals(t2.getName(), firstMatch.getTeam2().getName());
    }
}
