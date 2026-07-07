package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import abe.fvjc.tournament.domain.schedule.MatchResult;
import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.team.TeamRef;
import abe.fvjc.tournament.domain.common.problem.BusinessException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.domain.bracket.BracketValidator.validateBracketGenerateRequest;
import static abe.fvjc.tournament.domain.bracket.BracketValidator.validateBracketMatchResult;

@Service
@RequiredArgsConstructor
public class BracketService {
    private final BracketMatchStore bracketMatchStore;
    private final BracketRoundStore bracketRoundStore;
    private final GroupStore groupStore;
    private final GroupRankingService groupRankingService;
    private final TournamentSearchService tournamentSearchService;

    public List<BracketRound> generate(final UUID tournamentId, final BracketGenerateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        final var groups = groupStore.findAllByTournamentId(TournamentId.of(tournamentId));
        final var groupRankings = groupRankingService.computeAllGroupRankings(tournamentId, List.of());
        validateBracketGenerateRequest(request, groups.size());

        final var qualifiersPerGroup = request.getTotalQualifiedTeams() / groups.size();
        final var extraQualifiers = request.getTotalQualifiedTeams() % groups.size();

        if (extraQualifiers > 0) {
            final var eligibleGroups = groupRankings.stream()
                    .filter(r -> r.getEntries().size() > qualifiersPerGroup)
                    .count();
            if (eligibleGroups < extraQualifiers) {
                throw new BusinessException("Pas assez d'équipes dans les groupes pour sélectionner "
                        + extraQualifiers + " qualifié(s) supplémentaire(s)");
            }
        }

        final var existingRounds = bracketRoundStore.findAllByTournamentId(TournamentId.of(tournamentId));
        existingRounds.forEach(r -> bracketMatchStore.deleteAllByRoundId(r.getId()));
        bracketRoundStore.deleteAllByTournamentId(TournamentId.of(tournamentId));

        final var round1Pairs = buildRound1Pairs(groupRankings, qualifiersPerGroup, extraQualifiers, request.getTieBreaker());
        final var totalTeams = request.getTotalQualifiedTeams();
        final var totalRounds = (int) (Math.log(totalTeams) / Math.log(2));
        final var firstRoundStart = LocalDateTime.of(tournament.getDate(), request.getStartTime());

        final var matchIdsByRound = preassignMatchIds(round1Pairs.size(), totalRounds);
        final var troisiemePlaceMatchId = totalRounds >= 2 ? generateBracketMatchId() : null;

        final var savedRounds = new ArrayList<BracketRound>();
        final var matchesByRound = new ArrayList<List<BracketMatch>>();

        for (int r = 0; r < totalRounds; r++) {
            final var roundStart = firstRoundStart.plusMinutes(
                    (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var teamsInRound = totalTeams / (int) Math.pow(2, r);
            final var isDemiFinale = teamsInRound == 4;
            final var roundToSave = BracketRound.builder()
                    .id(BracketRoundId.of(UUID.randomUUID()))
                    .tournamentId(TournamentId.of(tournamentId))
                    .number(r + 1)
                    .name(roundName(teamsInRound))
                    .startTime(roundStart)
                    .matches(List.of())
                    .build();
            final var round = bracketRoundStore.save(roundToSave);

            final var matchIds = matchIdsByRound.get(r);
            final var roundMatches = new ArrayList<BracketMatch>();

            for (int m = 0; m < matchIds.size(); m++) {
                final var nextMatchId = (r < totalRounds - 1)
                        ? matchIdsByRound.get(r + 1).get(m / 2)
                        : null;
                final var slot = (m % 2) + 1;
                final var team1 = (r == 0) ? round1Pairs.get(m).first() : null;
                final var team2 = (r == 0) ? round1Pairs.get(m).second() : null;
                final var field = (m % tournament.getNumberOfFields()) + 1;
                final var loserNextMatchId = isDemiFinale ? troisiemePlaceMatchId : null;
                final var loserNextMatchTeamSlot = isDemiFinale ? slot : 0;

                final var matchToSave = BracketMatch.builder()
                        .id(matchIds.get(m))
                        .roundId(round.getId())
                        .field(field)
                        .team1(team1)
                        .team2(team2)
                        .result(null)
                        .nextMatchId(nextMatchId)
                        .nextMatchTeamSlot(slot)
                        .loserNextMatchId(loserNextMatchId)
                        .loserNextMatchTeamSlot(loserNextMatchTeamSlot)
                        .build();
                final var match = bracketMatchStore.save(matchToSave);
                roundMatches.add(match);
            }
            savedRounds.add(round);
            matchesByRound.add(roundMatches);
        }

        if (totalRounds >= 2) {
            final var troisiemePlaceStart = firstRoundStart.plusMinutes(
                    (long) totalRounds * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var thirdPlaceRound = BracketRound.builder()
                    .id(BracketRoundId.of(UUID.randomUUID()))
                    .tournamentId(TournamentId.of(tournamentId))
                    .number(totalRounds + 1)
                    .name("Troisième place")
                    .startTime(troisiemePlaceStart)
                    .matches(List.of())
                    .build();
            final var troisiemePlaceRound = bracketRoundStore.save(thirdPlaceRound);
            final var thirdPlaceMatch = BracketMatch.builder()
                    .id(troisiemePlaceMatchId)
                    .roundId(troisiemePlaceRound.getId())
                    .field(1)
                    .team1(null)
                    .team2(null)
                    .result(null)
                    .nextMatchId(null)
                    .nextMatchTeamSlot(0)
                    .loserNextMatchId(null)
                    .loserNextMatchTeamSlot(0)
                    .build();
            final var troisiemePlaceMatch = bracketMatchStore.save(thirdPlaceMatch);
            savedRounds.add(troisiemePlaceRound);
            matchesByRound.add(List.of(troisiemePlaceMatch));
        }

        return savedRounds.stream()
                .map(r -> r.withMatches(matchesByRound.get(r.getNumber() - 1)))
                .toList();
    }

    public List<BracketRound> findAll(final UUID tournamentId) {
        return bracketRoundStore.findAllByTournamentId(TournamentId.of(tournamentId)).stream()
                .map(r -> r.withMatches(bracketMatchStore.findAllByRoundId(r.getId())))
                .toList();
    }

    public BracketMatch enterResult(final UUID matchId, final BracketMatchResultRequest request) {
        validateBracketMatchResult(request);

        final var match = findBracketMatchById(BracketMatchId.of(matchId));

        final var result = MatchResult.builder()
                .score1(request.getScore1())
                .score2(request.getScore2())
                .build();
        final var savedMatch = bracketMatchStore.save(match.withResult(result));

        final var winner = request.getScore1() > request.getScore2()
                ? match.getTeam1()
                : match.getTeam2();
        final var loser = request.getScore1() > request.getScore2()
                ? match.getTeam2()
                : match.getTeam1();

        if (savedMatch.getNextMatchId() != null) {
            final var nextMatch = findBracketMatchById(savedMatch.getNextMatchId());
            final var updatedNextMatch = savedMatch.getNextMatchTeamSlot() == 1
                    ? nextMatch.withTeam1(winner)
                    : nextMatch.withTeam2(winner);
            bracketMatchStore.save(updatedNextMatch);
        }

        if (savedMatch.getLoserNextMatchId() != null) {
            final var loserMatch = findBracketMatchById(savedMatch.getLoserNextMatchId());
            final var updatedLoserMatch = savedMatch.getLoserNextMatchTeamSlot() == 1
                    ? loserMatch.withTeam1(loser)
                    : loserMatch.withTeam2(loser);
            bracketMatchStore.save(updatedLoserMatch);
        }

        return savedMatch;
    }

    private BracketMatch findBracketMatchById(BracketMatchId matchId) {
        return bracketMatchStore.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match", matchId));
    }

    private static List<List<BracketMatchId>> preassignMatchIds(final int round1Count, final int totalRounds) {
        final var result = new ArrayList<List<BracketMatchId>>();
        for (int round = 0; round < totalRounds; round++) {
            final var count = round1Count / (int) Math.pow(2, round);
            final var ids = new ArrayList<BracketMatchId>();
            for (int i = 0; i < count; i++) {
                ids.add(generateBracketMatchId());
            }
            result.add(ids);
        }
        return result;
    }

    private static BracketMatchId generateBracketMatchId() {
        return BracketMatchId.of(UUID.randomUUID());
    }

    private static List<Pair> buildRound1Pairs(
            final List<GroupRanking> rankings,
            final int qualifiersPerGroup,
            final int extraQualifiers,
            final TieBreaker tieBreaker) {
        final var seeded = buildSeededList(rankings, qualifiersPerGroup, extraQualifiers, tieBreaker);
        return pairSeeded(seeded);
    }

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

    private static List<Pair> pairSeeded(final List<TeamRef> seeded) {
        final var n = seeded.size();
        final var pairs = new ArrayList<Pair>();
        for (int i = 0; i < n / 2; i++) {
            pairs.add(new Pair(seeded.get(i), seeded.get(n - 1 - i)));
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

    private static String roundName(final int teamsCount) {
        return switch (teamsCount) {
            case 16 -> "Huitièmes de finale";
            case 8 -> "Quarts de finale";
            case 4 -> "Demi-finales";
            case 2 -> "Finale";
            default -> "Tour de " + teamsCount;
        };
    }

    private record Pair(TeamRef first, TeamRef second) {}
}
