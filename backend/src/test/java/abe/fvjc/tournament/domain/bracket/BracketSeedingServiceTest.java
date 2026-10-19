package abe.fvjc.tournament.domain.bracket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.Stream;

import static abe.fvjc.tournament.domain.bracket.TieBreaker.POINTS_DIFF;
import static abe.fvjc.tournament.domain.bracket.TieBreaker.POINTS_SCORED;
import static abe.fvjc.tournament.domain.bracket.TieBreaker.POINTS_TAKEN;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGroupRanking;
import static abe.fvjc.tournament.domain.fakes.BracketFakes.buildGroupRankingWithRunnerUpScores;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class BracketSeedingServiceTest {

    @InjectMocks
    private BracketSeedingService bracketSeedingService;

    @Test
    void seedFirstRoundWhenTwoQualifiersPerGroupShouldPairWinnersAgainstRunnersUp() {
        final var rankings = List.of(
                buildGroupRanking("A", 3),
                buildGroupRanking("B", 3),
                buildGroupRanking("C", 3),
                buildGroupRanking("D", 3));

        final var pairsSeeded = bracketSeedingService.seedFirstRound(rankings, 2, 0, POINTS_DIFF);

        assertEquals(4, pairsSeeded.size());
        pairsSeeded.forEach(pair -> {
            assertTrue(pair.team1().getName().endsWith("1"), "team1 should be a group winner: " + pair.team1().getName());
            assertTrue(pair.team2().getName().endsWith("2"), "team2 should be a runner-up: " + pair.team2().getName());
        });
    }

    @Test
    void seedFirstRoundWhenOneQualifierPerGroupShouldSeedEveryWinnerOnce() {
        final var rankings = List.of(
                buildGroupRanking("A", 2),
                buildGroupRanking("B", 2),
                buildGroupRanking("C", 2),
                buildGroupRanking("D", 2));

        final var pairsSeeded = bracketSeedingService.seedFirstRound(rankings, 1, 0, POINTS_DIFF);

        final var seededNames = teamNames(pairsSeeded);

        assertEquals(2, pairsSeeded.size());
        assertEquals(List.of("A1", "B1", "C1", "D1"), seededNames);
    }

    @Test
    void seedFirstRoundWhenExtraQualifierByPointsDiffShouldPickBestRunnerUpAgainstAWinner() {
        final var rankings = List.of(
                buildGroupRankingWithRunnerUpScores("A", 6, 8),
                buildGroupRankingWithRunnerUpScores("B", 9, 5),
                buildGroupRankingWithRunnerUpScores("C", 7, 6));

        final var pairsSeeded = bracketSeedingService.seedFirstRound(rankings, 1, 1, POINTS_DIFF);

        final var seededNames = teamNames(pairsSeeded);
        final var topSeedPair = pairsSeeded.getFirst();

        assertEquals(2, pairsSeeded.size());
        assertEquals(List.of("A1", "B1", "B2", "C1"), seededNames);
        assertTrue(topSeedPair.team1().getName().endsWith("1"));
        assertEquals("B2", topSeedPair.team2().getName());
    }

    @Test
    void seedFirstRoundWhenExtraQualifierByPointsScoredShouldPickRunnerUpWithMostGoalsFor() {
        final var rankings = List.of(
                buildGroupRankingWithRunnerUpScores("A", 12, 20),
                buildGroupRankingWithRunnerUpScores("B", 9, 5),
                buildGroupRankingWithRunnerUpScores("C", 7, 6));

        final var pairsSeeded = bracketSeedingService.seedFirstRound(rankings, 1, 1, POINTS_SCORED);

        final var seededNames = teamNames(pairsSeeded);

        assertEquals(List.of("A1", "A2", "B1", "C1"), seededNames);
    }

    @Test
    void seedFirstRoundWhenExtraQualifierByPointsTakenShouldPickRunnerUpWithFewestGoalsAgainst() {
        final var rankings = List.of(
                buildGroupRankingWithRunnerUpScores("A", 12, 20),
                buildGroupRankingWithRunnerUpScores("B", 9, 5),
                buildGroupRankingWithRunnerUpScores("C", 1, 2));

        final var pairsSeeded = bracketSeedingService.seedFirstRound(rankings, 1, 1, POINTS_TAKEN);

        final var seededNames = teamNames(pairsSeeded);

        assertEquals(List.of("A1", "B1", "C1", "C2"), seededNames);
    }

    private static List<String> teamNames(final List<SeedPair> pairs) {
        return pairs.stream()
                .flatMap(pair -> Stream.of(pair.team1().getName(), pair.team2().getName()))
                .sorted()
                .toList();
    }
}
