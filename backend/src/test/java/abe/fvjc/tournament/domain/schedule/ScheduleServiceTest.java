package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.fakes.GroupFakes;
import abe.fvjc.tournament.domain.fakes.ScheduleFakes;
import abe.fvjc.tournament.domain.fakes.TeamFakes;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildGenerateRequest;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildMatch;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildMatchPair;
import static abe.fvjc.tournament.domain.fakes.ScheduleFakes.buildRound;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static abe.fvjc.tournament.domain.tournament.TournamentStatus.IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
    private RoundRobinService roundRobinService;

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
    void generateWhenValidShouldBuildRoundsAndFieldsFromPlan() {
        final var tournament = buildTournament();
        final var org = OrganisationId.of(UUID.randomUUID());
        final var group = GroupFakes.buildGroup(tournament.getId()).withName("A");
        final var t1 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(group.getId());
        final var t2 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(group.getId());
        final var t3 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(group.getId());
        final var t4 = TeamFakes.buildTeam(org, tournament.getId()).withGroupId(group.getId());
        final var plan = List.of(
                List.of(buildMatchPair(group.getId(), t1.getId(), t2.getId()), buildMatchPair(group.getId(), t3.getId(), t4.getId())),
                List.of(buildMatchPair(group.getId(), t1.getId(), t3.getId())));
        final var request = buildGenerateRequest();

        when(tournamentSearchService.findById(tournament.getId())).thenReturn(tournament);
        when(groupStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(group));
        when(roundStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of());
        when(teamStore.findAllByTournamentId(tournament.getId())).thenReturn(List.of(t1, t2, t3, t4));
        when(roundRobinService.planRounds(Map.of(group.getId(), List.of(t1.getId(), t2.getId(), t3.getId(), t4.getId())), 4))
                .thenReturn(plan);

        final var scheduleGenerated = scheduleService.generate(tournament.getId(), request);

        verify(roundRobinService).planRounds(anyMap(), eq(4));
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());

        assertEquals(2, scheduleGenerated.getTotalRounds());
        assertEquals(3, scheduleGenerated.getTotalMatches());
        assertEquals(1, scheduleGenerated.getRounds().get(0).getNumber());
        assertEquals(LocalDateTime.of(2027, 8, 15, 9, 0), scheduleGenerated.getRounds().get(0).getStartTime());
        assertEquals(LocalDateTime.of(2027, 8, 15, 9, 25), scheduleGenerated.getRounds().get(1).getStartTime());
        assertEquals(2, scheduleGenerated.getRounds().get(0).getMatches().get(1).getField());
        assertEquals(t3.getName(), scheduleGenerated.getRounds().get(0).getMatches().get(1).getTeam1().getName());
        assertEquals("A", scheduleGenerated.getRounds().get(1).getMatches().get(0).getGroupName());
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
        when(roundRobinService.planRounds(anyMap(), eq(4))).thenReturn(List.of(List.of(buildMatchPair(groupId, t1.getId(), t2.getId()))));

        scheduleService.generate(tournament.getId(), request);

        verify(roundRobinService).planRounds(anyMap(), eq(4));
        verify(matchStore).deleteAllByRoundIds(anyList());
        verify(roundStore).deleteAllByTournamentId(tournament.getId());
        verify(roundStore).saveAll(anyList());
        verify(matchStore).saveAll(anyList());
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
