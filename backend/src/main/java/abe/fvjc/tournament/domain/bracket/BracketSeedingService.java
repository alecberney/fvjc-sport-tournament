package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import abe.fvjc.tournament.domain.team.TeamRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pure bracket seeding algorithm: no store, no side effect.
 * Picks the qualified teams from the group rankings, seeds them and pairs
 * the first round so the best seeds meet the weakest ones.
 */
@Service
public class BracketSeedingService {

    /**
     * Seeds the first round of the bracket.
     *
     * @param rankings           final ranking of each group
     * @param qualifiersPerGroup teams qualified directly from every group (top N)
     * @param extraQualifiers    additional teams picked among the next-placed teams by tie-breaker
     * @param tieBreaker         criterion used to rank the extra qualifiers across groups
     * @return the first-round pairs, in bracket order
     */
    public List<SeedPair> seedFirstRound(
            final List<GroupRanking> rankings,
            final int qualifiersPerGroup,
            final int extraQualifiers,
            final TieBreaker tieBreaker) {
        final var seeded = buildSeededList(rankings, qualifiersPerGroup, extraQualifiers, tieBreaker);
        return pairSeeded(seeded);
    }

    // Seeds are built position by position: all group winners first, then all runners-up, ...
    // Each "hat" (same position) is shuffled so teams of equal rank are drawn randomly.
    // Extra qualifiers come last, chosen by tie-breaker among the next position.
    private static List<TeamRef> buildSeededList(
            final List<GroupRanking> rankings,
            final int qualifiersPerGroup,
            final int extraQualifiers,
            final TieBreaker tieBreaker) {
        final var seeded = new ArrayList<TeamRef>();
        for (int pos = 0; pos < qualifiersPerGroup; pos++) {
            final var position = pos;
            final var hat = new ArrayList<>(rankings.stream()
                    .map(r -> r.getEntries().get(position).getTeam())
                    .toList());
            Collections.shuffle(hat);
            seeded.addAll(hat);
        }
        if (extraQualifiers > 0) {
            final var extras = new ArrayList<>(rankings.stream()
                    .filter(r -> r.getEntries().size() > qualifiersPerGroup)
                    .map(r -> r.getEntries().get(qualifiersPerGroup))
                    .sorted(tieBreakerComparator(tieBreaker))
                    .limit(extraQualifiers)
                    .map(GroupRankingEntry::getTeam)
                    .toList());
            Collections.shuffle(extras);
            seeded.addAll(extras);
        }
        return seeded;
    }

    // Best seed against worst seed: 1 vs n, 2 vs n-1, ...
    private static List<SeedPair> pairSeeded(final List<TeamRef> seeded) {
        final var n = seeded.size();
        final var pairs = new ArrayList<SeedPair>();
        for (int i = 0; i < n / 2; i++) {
            pairs.add(new SeedPair(seeded.get(i), seeded.get(n - 1 - i)));
        }
        return pairs;
    }

    private static Comparator<GroupRankingEntry> tieBreakerComparator(final TieBreaker tieBreaker) {
        return switch (tieBreaker) {
            case POINTS_SCORED -> Comparator.comparingInt(GroupRankingEntry::getGoalsFor).reversed();
            case POINTS_DIFF -> Comparator.comparingInt(GroupRankingEntry::getGoalDifference).reversed();
            case POINTS_TAKEN -> Comparator.comparingInt(GroupRankingEntry::getGoalsAgainst);
        };
    }
}
