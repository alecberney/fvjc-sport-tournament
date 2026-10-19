package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.team.TeamId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Pure group-stage scheduling algorithm: no store, no side effect.
 * Builds every round-robin pairing of each group, then packs them into rounds
 * of at most {@code numberOfFields} matches, never scheduling a team twice in a round.
 */
@Service
public class RoundRobinService {

    /**
     * Plans the whole group stage.
     *
     * @param teamIdsByGroupId teams of each group, iterated in map order
     * @param numberOfFields   maximum number of matches played simultaneously
     * @return one list of match pairs per round, in play order; the index in a round is the field
     */
    public List<List<MatchPair>> planRounds(final Map<GroupId, List<TeamId>> teamIdsByGroupId, final int numberOfFields) {
        final var allMatches = new ArrayList<MatchPair>();
        teamIdsByGroupId.forEach((groupId, teamIds) -> allMatches.addAll(generateGroupMatches(groupId, teamIds)));
        return packRounds(allMatches, numberOfFields);
    }

    // Circle method: one team stays fixed while the others rotate, which yields
    // every pairing exactly once. An odd group gets a null "bye" slot that is skipped.
    private static List<MatchPair> generateGroupMatches(final GroupId groupId, final List<TeamId> teams) {
        final var circle = new ArrayList<>(teams);
        if (teams.size() % 2 != 0) {
            circle.add(null);
        }
        final var circleSize = circle.size();
        final var pairs = new ArrayList<MatchPair>();
        for (int r = 0; r < circleSize - 1; r++) {
            for (int m = 0; m < circleSize / 2; m++) {
                final var t1 = circle.get(m);
                final var t2 = circle.get(circleSize - 1 - m);
                if (t1 != null && t2 != null) {
                    pairs.add(new MatchPair(groupId, t1, t2));
                }
            }
            final var last = circle.remove(circleSize - 1);
            circle.add(1, last);
        }
        return pairs;
    }

    private static List<List<MatchPair>> packRounds(final List<MatchPair> allMatches, final int numFields) {
        final var remaining = new ArrayList<>(allMatches);
        final var rounds = new ArrayList<List<MatchPair>>();
        // Round index in which each team last played. Absent = has not played yet.
        // Used to measure "rest" (rounds waited) and to detect back-to-back play.
        final var lastPlayedRound = new HashMap<TeamId, Integer>();
        var roundIndex = 0;
        while (!remaining.isEmpty()) {
            final var degrees = computeDegrees(remaining);
            final var currentRound = roundIndex;
            // Order remaining matches so the fairest one to play now sorts first:
            //   1. Matches with NO team that played the previous round come first,
            //      so we avoid back-to-back play whenever an alternative exists.
            //   2. Among equals, the match whose least-rested team has waited the
            //      longest comes first (min rest across the two teams, descending),
            //      so the most starved team is served first and idle waits stay short.
            //   3. Finally fall back to the remaining-degree heuristic (teams with
            //      the most matches left first) so the total round count stays minimal.
            remaining.sort((a, b) -> {
                final var backToBackA = isBackToBack(a, lastPlayedRound, currentRound);
                final var backToBackB = isBackToBack(b, lastPlayedRound, currentRound);
                if (backToBackA != backToBackB) {
                    return Boolean.compare(backToBackA, backToBackB);
                }
                final var restA = minRest(a, lastPlayedRound, currentRound);
                final var restB = minRest(b, lastPlayedRound, currentRound);
                if (restA != restB) {
                    return Integer.compare(restB, restA);
                }
                final var degreeA = degrees.get(a.team1Id()) + degrees.get(a.team2Id());
                final var degreeB = degrees.get(b.team1Id()) + degrees.get(b.team2Id());
                return Integer.compare(degreeB, degreeA);
            });
            final var round = new ArrayList<MatchPair>();
            final var usedTeams = new HashSet<TeamId>();
            final var iterator = remaining.iterator();
            while (iterator.hasNext() && round.size() < numFields) {
                final var pair = iterator.next();
                final var team1Free = !usedTeams.contains(pair.team1Id());
                final var team2Free = !usedTeams.contains(pair.team2Id());
                if (team1Free && team2Free) {
                    round.add(pair);
                    usedTeams.add(pair.team1Id());
                    usedTeams.add(pair.team2Id());
                    iterator.remove();
                }
            }
            // Every team that played is now marked as having played this round,
            // so later rounds can measure their rest and skip back-to-back matches.
            for (final var team : usedTeams) {
                lastPlayedRound.put(team, currentRound);
            }
            rounds.add(round);
            roundIndex++;
        }
        return rounds;
    }

    // A match is "back-to-back" when either of its teams played in the immediately
    // previous round. Such matches are only used to fill a field that would
    // otherwise sit empty. MIN_VALUE default keeps never-played teams from matching
    // the previous round (-1) in the very first round.
    private static boolean isBackToBack(
            final MatchPair pair,
            final Map<TeamId, Integer> lastPlayedRound,
            final int roundIndex) {
        final var previousRound = roundIndex - 1;
        return lastPlayedRound.getOrDefault(pair.team1Id(), Integer.MIN_VALUE) == previousRound
                || lastPlayedRound.getOrDefault(pair.team2Id(), Integer.MIN_VALUE) == previousRound;
    }

    // Rest = rounds since a team last played (a never-played team counts as fully
    // rested via the -1 default). A match's rest is the minimum across its two
    // teams, so the more starved team drives how urgently the match is scheduled.
    private static int minRest(
            final MatchPair pair,
            final Map<TeamId, Integer> lastPlayedRound,
            final int roundIndex) {
        final var rest1 = roundIndex - lastPlayedRound.getOrDefault(pair.team1Id(), -1);
        final var rest2 = roundIndex - lastPlayedRound.getOrDefault(pair.team2Id(), -1);
        return Math.min(rest1, rest2);
    }

    private static Map<TeamId, Integer> computeDegrees(final List<MatchPair> matches) {
        final var degrees = new HashMap<TeamId, Integer>();
        for (final var pair : matches) {
            degrees.merge(pair.team1Id(), 1, Integer::sum);
            degrees.merge(pair.team2Id(), 1, Integer::sum);
        }
        return degrees;
    }
}
