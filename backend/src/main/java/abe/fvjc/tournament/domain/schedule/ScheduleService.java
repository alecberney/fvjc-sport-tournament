package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamId;

import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.domain.schedule.ScheduleValidator.validateScheduleGenerateRequest;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final RoundStore roundStore;
    private final TeamStore teamStore;
    private final TournamentSearchService tournamentSearchService;

    public ScheduleOverview generate(final TournamentId tournamentId, final ScheduleGenerateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new ConflictException("Le calendrier ne peut être généré que pour un tournoi en préparation");
        }
        validateScheduleGenerateRequest(request);
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        if (groups.isEmpty()) {
            throw new ValidationException(List.of(new ValidationException.FieldError(
                    "groups", "Aucun groupe n'a été généré pour ce tournoi")));
        }
        final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
        if (!existingRounds.isEmpty()) {
            final var existingRoundIds = existingRounds.stream()
                    .map(Round::getId)
                    .toList();
            if (matchStore.existsResultByRoundIds(existingRoundIds)) {
                throw new ConflictException("Impossible de régénérer le calendrier : des résultats ont déjà été saisis");
            }
            deleteAllByTournamentId(tournamentId);
        }
        final var allTeams = teamStore.findAllByTournamentId(tournamentId);
        final var teamsByGroupId = allTeams.stream()
                .collect(Collectors.groupingBy(t -> t.getGroupId().value()));
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var allMatches = new ArrayList<MatchPair>();
        for (final var group : groups) {
            final var groupTeams = teamsByGroupId.getOrDefault(group.getId().value(), List.of());
            final var teamIds = groupTeams.stream().map(Team::getId).toList();
            allMatches.addAll(generateGroupMatches(group.getId(), teamIds));
        }
        final var packedRounds = packRounds(allMatches, tournament.getNumberOfFields());
        final var totalRounds = packedRounds.size();
        final var startTime = LocalTime.parse(request.getStartTime(), DateTimeFormatter.ofPattern("HH:mm"));
        final var firstRoundStart = LocalDateTime.of(tournament.getDate(), startTime);
        final var rounds = new ArrayList<Round>();
        final var matches = new ArrayList<Match>();
        for (int r = 0; r < totalRounds; r++) {
            final var roundStart = firstRoundStart.plusMinutes(
                    (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var roundId = RoundId.of(UUID.randomUUID());
            rounds.add(buildRound(tournamentId, roundId, r + 1, roundStart));
            final var roundMatches = packedRounds.get(r);
            for (int f = 0; f < roundMatches.size(); f++) {
                final var pair = roundMatches.get(f);
                matches.add(buildMatch(roundId, f + 1, pair));
            }
        }
        roundStore.saveAll(rounds);
        matchStore.saveAll(matches);
        return toScheduleOverview(rounds, matches, groupById, teamById);
    }

    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        final var existingRounds = roundStore.findAllByTournamentId(tournamentId);
        if (existingRounds.isEmpty()) {
            return;
        }
        final var existingRoundIds = existingRounds.stream()
                .map(Round::getId)
                .toList();
        matchStore.deleteAllByRoundIds(existingRoundIds);
        roundStore.deleteAllByTournamentId(tournamentId);
    }

    public ScheduleOverview findByTournamentId(final TournamentId tournamentId) {
        final var rounds = roundStore.findAllByTournamentId(tournamentId);
        if (rounds.isEmpty()) {
            return ScheduleOverview.empty();
        }
        final var roundIds = rounds.stream().map(Round::getId).toList();
        final var matches = matchStore.findAllByRoundIds(roundIds);
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var allTeams = teamStore.findAllByTournamentId(tournamentId);
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return toScheduleOverview(rounds, matches, groupById, teamById);
    }

    private static Round buildRound(
            final TournamentId tournamentId,
            final RoundId roundId,
            final int number,
            final LocalDateTime startTime) {
        return Round.builder()
                .id(roundId)
                .tournamentId(tournamentId)
                .number(number)
                .startTime(startTime)
                .build();
    }

    private static Match buildMatch(final RoundId roundId, final int field, final MatchPair pair) {
        return Match.builder()
                .id(MatchId.of(UUID.randomUUID()))
                .roundId(roundId)
                .field(field)
                .groupId(pair.groupId())
                .team1Id(pair.team1Id())
                .team2Id(pair.team2Id())
                .build();
    }

    private static ScheduleOverview toScheduleOverview(
            final List<Round> rounds,
            final List<Match> matches,
            final Map<UUID, Group> groupById,
            final Map<UUID, Team> teamById) {
        final var matchesByRoundId = matches.stream()
                .collect(Collectors.groupingBy(m -> m.getRoundId().value()));
        final var roundOverviews = rounds.stream()
                .map(round -> {
                    final var roundMatches = matchesByRoundId.getOrDefault(round.getId().value(), List.<Match>of());
                    final var matchOverviews = roundMatches.stream()
                            .map(match -> toMatchOverview(match, groupById, teamById))
                            .toList();
                    return toRoundOverview(round, matchOverviews);
                })
                .toList();
        return ScheduleOverview.builder()
                .totalRounds(rounds.size())
                .totalMatches(matches.size())
                .rounds(roundOverviews)
                .build();
    }

    private static MatchOverview toMatchOverview(
            final Match match,
            final Map<UUID, Group> groupById,
            final Map<UUID, Team> teamById) {
        return MatchOverview.builder()
                .id(match.getId())
                .field(match.getField())
                .groupId(match.getGroupId())
                .groupName(groupById.get(match.getGroupId().value()).getName())
                .team1(toTeamRef(match.getTeam1Id(), teamById.get(match.getTeam1Id().value()).getName()))
                .team2(toTeamRef(match.getTeam2Id(), teamById.get(match.getTeam2Id().value()).getName()))
                .result(match.getResult())
                .build();
    }

    private static RoundOverview toRoundOverview(final Round round, final List<MatchOverview> matches) {
        return RoundOverview.builder()
                .id(round.getId())
                .number(round.getNumber())
                .startTime(round.getStartTime())
                .matches(matches)
                .build();
    }

    private record MatchPair(GroupId groupId, TeamId team1Id, TeamId team2Id) {}

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
