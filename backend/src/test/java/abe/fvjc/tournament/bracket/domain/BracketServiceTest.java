package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.group.domain.GroupFakes;
import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.GroupRankingEntry;
import abe.fvjc.tournament.schedule.domain.RankingService;
import abe.fvjc.tournament.shared.exception.ValidationException;
import abe.fvjc.tournament.team.domain.TeamId;

import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;
import abe.fvjc.tournament.tournament.domain.TournamentFakes;
import abe.fvjc.tournament.tournament.domain.TournamentId;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private BracketMatchStore bracketMatchStore;

    @Mock
    private BracketRoundStore bracketRoundStore;

    @Mock
    private GroupStore groupStore;

    @Mock
    private RankingService rankingService;

    @Mock
    private TournamentStore tournamentStore;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void generateWhenValidRequestShouldCreateRoundsAndMatches() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequest();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");
        final var groups = List.of(gA, gB, gC, gD);

        final var rankings = List.of(
                buildRanking(gA.getId(), "A", 2),
                buildRanking(gB.getId(), "B", 2),
                buildRanking(gC.getId(), "C", 2),
                buildRanking(gD.getId(), "D", 2));

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(groups);
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(rankings);
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentId, request);

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(rankingService).computeAllGroupRankings(tournamentId, List.of());
        verify(bracketRoundStore, times(3)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(7)).save(any(BracketMatch.class));

        assertEquals(3, roundsGenerated.size());
        assertEquals("Quarts de finale", roundsGenerated.get(0).getName());
        assertEquals("Demi-finales", roundsGenerated.get(1).getName());
        assertEquals("Finale", roundsGenerated.get(2).getName());
        assertEquals(4, roundsGenerated.get(0).getMatches().size());
        assertEquals(2, roundsGenerated.get(1).getMatches().size());
        assertEquals(1, roundsGenerated.get(2).getMatches().size());
    }

    @Test
    void generateWhenTotalTeamsNotPowerOfTwoShouldThrowValidationException() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestNotPowerOfTwo();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(gA, gB, gC));
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 2),
                        buildRanking(gB.getId(), "B", 2),
                        buildRanking(gC.getId(), "C", 2)));

        final var exception = assertThrows(ValidationException.class,
                () -> bracketService.generate(tournamentId, request));

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(rankingService).computeAllGroupRankings(tournamentId, List.of());

        assertEquals(1, exception.getErrors().size());
        assertEquals("totalQualifiedTeams", exception.getErrors().get(0).field());
    }

    @Test
    void generateWhenFourTeamsQualifiedShouldCreateTwoRounds() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestWithFourTeams();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");
        final var gD = GroupFakes.buildGroup(tid).withName("D");

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(gA, gB, gC, gD));
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(
                List.of(buildRanking(gA.getId(), "A", 1),
                        buildRanking(gB.getId(), "B", 1),
                        buildRanking(gC.getId(), "C", 1),
                        buildRanking(gD.getId(), "D", 1)));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentId, request);

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(rankingService).computeAllGroupRankings(tournamentId, List.of());
        verify(bracketRoundStore, times(2)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));

        assertEquals(2, roundsGenerated.size());
        assertEquals("Demi-finales", roundsGenerated.get(0).getName());
        assertEquals("Finale", roundsGenerated.get(1).getName());
        assertEquals(2, roundsGenerated.get(0).getMatches().size());
        assertEquals(1, roundsGenerated.get(1).getMatches().size());
    }

    @Test
    void generateWhenExtraQualifiersShouldSelectByTieBreakerAndCreateBracket() {
        final var tournamentId = UUID.randomUUID();
        final var tid = TournamentId.of(tournamentId);
        final var tournament = TournamentFakes.buildTournament().withId(tid);
        final var request = BracketFakes.buildGenerateRequestWithExtras();

        final var gA = GroupFakes.buildGroup(tid).withName("A");
        final var gB = GroupFakes.buildGroup(tid).withName("B");
        final var gC = GroupFakes.buildGroup(tid).withName("C");

        when(tournamentStore.findById(tournamentId)).thenReturn(Optional.of(tournament));
        when(groupStore.findAllByTournamentId(tournamentId)).thenReturn(List.of(gA, gB, gC));
        when(rankingService.computeAllGroupRankings(tournamentId, List.of())).thenReturn(
                List.of(buildRankingWithExtras(gA.getId(), "A"),
                        buildRankingWithExtras(gB.getId(), "B"),
                        buildRankingWithExtras(gC.getId(), "C")));
        when(bracketRoundStore.save(any(BracketRound.class))).then(returnsFirstArg());
        when(bracketMatchStore.save(any(BracketMatch.class))).then(returnsFirstArg());

        final var roundsGenerated = bracketService.generate(tournamentId, request);

        verify(tournamentStore).findById(tournamentId);
        verify(groupStore).findAllByTournamentId(tournamentId);
        verify(rankingService).computeAllGroupRankings(tournamentId, List.of());
        verify(bracketRoundStore, times(2)).save(any(BracketRound.class));
        verify(bracketMatchStore, times(3)).save(any(BracketMatch.class));

        assertEquals(2, roundsGenerated.size());
        assertEquals("Demi-finales", roundsGenerated.get(0).getName());
        assertEquals("Finale", roundsGenerated.get(1).getName());
        assertEquals(2, roundsGenerated.get(0).getMatches().size());
        assertEquals(1, roundsGenerated.get(1).getMatches().size());
    }

    private static GroupRanking buildRanking(final GroupId groupId, final String groupName, final int numEntries) {
        final var entries = new ArrayList<GroupRankingEntry>();
        for (int i = 1; i <= numEntries; i++) {
            entries.add(GroupRankingEntry.builder()
                    .rank(i)
                    .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + i))
                    .goalsFor(10)
                    .goalsAgainst(5)
                    .goalDifference(5)
                    .build());
        }
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }

    private static GroupRanking buildRankingWithExtras(final GroupId groupId, final String groupName) {
        final var entries = List.of(
                GroupRankingEntry.builder()
                        .rank(1)
                        .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + "1"))
                        .goalsFor(10)
                        .goalsAgainst(5)
                        .goalDifference(5)
                        .build(),
                GroupRankingEntry.builder()
                        .rank(2)
                        .team(toTeamRef(TeamId.of(UUID.randomUUID()), groupName + "2"))
                        .goalsFor(6)
                        .goalsAgainst(8)
                        .goalDifference(-2)
                        .build());
        return GroupRanking.builder()
                .groupId(groupId)
                .groupName(groupName)
                .entries(entries)
                .build();
    }
}
