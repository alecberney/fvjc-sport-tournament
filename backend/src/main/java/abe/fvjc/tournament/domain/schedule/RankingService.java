package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamId;
import abe.fvjc.tournament.team.domain.TeamStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;

    public GroupRanking computeGroupRanking(final UUID tournamentId, final UUID groupId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var group = groups.stream()
                .filter(g -> g.getId().value().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Group", groupId));
        final var teams = teamStore.findAllByGroupId(groupId);
        final var matches = matchStore.findAllByGroupId(groupId);
        final var statsMap = buildStatsMap(teams);
        accumulateStats(matches, statsMap);
        final var entries = buildRankedEntries(teams, statsMap);
        return GroupRanking.builder()
                .groupId(GroupId.of(groupId))
                .groupName(group.getName())
                .entries(entries)
                .build();
    }

    private static Map<UUID, int[]> buildStatsMap(final List<Team> teams) {
        final var map = new HashMap<UUID, int[]>();
        for (final var team : teams) {
            map.put(team.getId().value(), new int[6]); // [played, wins, draws, defeats, goalsFor, goalsAgainst]
        }
        return map;
    }

    private static void accumulateStats(final List<Match> matches, final Map<UUID, int[]> statsMap) {
        for (final var match : matches) {
            if (match.getResult() == null) continue;
            final var r = match.getResult();
            final var s1 = statsMap.get(match.getTeam1Id().value());
            final var s2 = statsMap.get(match.getTeam2Id().value());
            if (s1 == null || s2 == null) continue;
            s1[0]++;
            s2[0]++;
            s1[4] += r.getScore1();
            s1[5] += r.getScore2();
            s2[4] += r.getScore2();
            s2[5] += r.getScore1();
            if (r.getScore1() > r.getScore2()) {
                s1[1]++;
                s2[3]++;
            } else if (r.getScore1() < r.getScore2()) {
                s2[1]++;
                s1[3]++;
            } else {
                s1[2]++;
                s2[2]++;
            }
        }
    }

    private static List<GroupRankingEntry> buildRankedEntries(
            final List<Team> teams, final Map<UUID, int[]> statsMap) {
        final var sorted = teams.stream()
                .sorted(Comparator
                        .comparingInt((Team t) -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[1] * 2 + s[2]);
                        })
                        .thenComparingInt(t -> {
                            final var s = statsMap.get(t.getId().value());
                            return -(s[4] - s[5]);
                        })
                        .thenComparingInt(t -> -statsMap.get(t.getId().value())[4]))
                .toList();
        final var entries = new ArrayList<GroupRankingEntry>();
        int rank = 1;
        for (int i = 0; i < sorted.size(); i++) {
            final var team = sorted.get(i);
            final var s = statsMap.get(team.getId().value());
            final var points = s[1] * 2 + s[2];
            final var goalDiff = s[4] - s[5];
            if (i > 0) {
                final var prev = sorted.get(i - 1);
                final var ps = statsMap.get(prev.getId().value());
                final var prevPoints = ps[1] * 2 + ps[2];
                final var prevGoalDiff = ps[4] - ps[5];
                if (points != prevPoints || goalDiff != prevGoalDiff || s[4] != ps[4]) {
                    rank = i + 1;
                }
            }
            entries.add(GroupRankingEntry.builder()
                    .rank(rank)
                    .team(TeamRef.builder()
                            .id(TeamId.of(team.getId().value()))
                            .name(team.getName())
                            .build())
                    .played(s[0])
                    .wins(s[1])
                    .draws(s[2])
                    .defeats(s[3])
                    .goalsFor(s[4])
                    .goalsAgainst(s[5])
                    .goalDifference(goalDiff)
                    .points(points)
                    .build());
        }
        return entries;
    }
}
