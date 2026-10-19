package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.bracket.BracketGenerateRequest;
import abe.fvjc.tournament.domain.bracket.BracketMatch;
import abe.fvjc.tournament.domain.bracket.BracketMatchResultRequest;
import abe.fvjc.tournament.domain.bracket.BracketRound;
import abe.fvjc.tournament.domain.bracket.SeedPair;
import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.bracket.TieBreaker.POINTS_DIFF;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomBracketMatchId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomBracketRoundId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomGroupId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTeamId;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;

@UtilityClass
public class BracketFakes {

    public static BracketGenerateRequest buildGenerateRequest() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(8)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestNotPowerOfTwo() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(6)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithFourTeams() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketGenerateRequest buildGenerateRequestWithExtras() {
        return BracketGenerateRequest.builder()
                .totalQualifiedTeams(4)
                .tieBreaker(POINTS_DIFF)
                .startTime("14:00")
                .matchDurationMinutes(20)
                .breakDurationMinutes(5)
                .build();
    }

    public static BracketMatch buildMatch() {
        return BracketMatch.builder()
                .id(randomBracketMatchId())
                .roundId(randomBracketRoundId())
                .field(1)
                .team1(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 1"))
                .team2(toTeamRef(TeamId.of(UUID.randomUUID()), "Équipe 2"))
                .result(null)
                .nextMatchId(randomBracketMatchId())
                .nextMatchTeamSlot(1)
                .loserNextMatchId(null)
                .loserNextMatchTeamSlot(0)
                .build();
    }

    public static BracketMatchResultRequest buildMatchResultRequest() {
        return BracketMatchResultRequest.builder()
                .score1(3)
                .score2(1)
                .build();
    }

    public static BracketRound buildRound(final TournamentId tournamentId, final int number) {
        return BracketRound.builder()
                .id(randomBracketRoundId())
                .tournamentId(tournamentId)
                .number(number)
                .name("Tour " + number)
                .startTime(LocalDateTime.of(2027, 8, 15, 14, 0))
                .build();
    }

    public static GroupRanking buildGroupRanking(final String groupName, final int numEntries) {
        final var entries = new ArrayList<GroupRankingEntry>();
        for (int i = 1; i <= numEntries; i++) {
            entries.add(buildGroupRankingEntry(i, groupName + i, 10, 5));
        }
        return GroupRanking.builder()
                .groupId(randomGroupId())
                .groupName(groupName)
                .entries(entries)
                .build();
    }

    public static GroupRanking buildGroupRankingWithExtras(final String groupName) {
        return buildGroupRankingWithRunnerUpScores(groupName, 6, 8);
    }

    public static GroupRanking buildGroupRankingWithRunnerUpScores(
            final String groupName,
            final int goalsFor,
            final int goalsAgainst) {
        final var entries = List.of(
                buildGroupRankingEntry(1, groupName + "1", 10, 5),
                buildGroupRankingEntry(2, groupName + "2", goalsFor, goalsAgainst));
        return GroupRanking.builder()
                .groupId(randomGroupId())
                .groupName(groupName)
                .entries(entries)
                .build();
    }

    public static List<SeedPair> buildSeedPairs(final int count) {
        final var pairs = new ArrayList<SeedPair>();
        for (int i = 1; i <= count; i++) {
            pairs.add(new SeedPair(
                    toTeamRef(randomTeamId(), "Équipe " + (2 * i - 1)),
                    toTeamRef(randomTeamId(), "Équipe " + (2 * i))));
        }
        return pairs;
    }

    private static GroupRankingEntry buildGroupRankingEntry(
            final int rank,
            final String teamName,
            final int goalsFor,
            final int goalsAgainst) {
        return GroupRankingEntry.builder()
                .rank(rank)
                .team(toTeamRef(randomTeamId(), teamName))
                .goalsFor(goalsFor)
                .goalsAgainst(goalsAgainst)
                .goalDifference(goalsFor - goalsAgainst)
                .build();
    }
}
