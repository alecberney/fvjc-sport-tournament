package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.ValidationException;
import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.team.TeamId;

import static abe.fvjc.tournament.domain.group.GroupRef.toGroupRef;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;
import abe.fvjc.tournament.domain.team.TeamStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.domain.schedule.ScheduleValidator.validateScheduleGenerateRequest;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final RoundStore roundStore;
    private final TeamStore teamStore;
    private final TournamentSearchService tournamentSearchService;

    public ScheduleOverview generate(final UUID tournamentId, final ScheduleGenerateRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new ConflictException("Le calendrier ne peut être généré que pour un tournoi en préparation");
        }
        validateScheduleGenerateRequest(request);
        final var groups = groupStore.findAllByTournamentId(TournamentId.of(tournamentId));
        if (groups.isEmpty()) {
            throw new ValidationException(List.of(new ValidationException.FieldError(
                    "groups", "Aucun groupe n'a été généré pour ce tournoi")));
        }
        final var existingRounds = roundStore.findAllByTournamentId(TournamentId.of(tournamentId));
        if (!existingRounds.isEmpty()) {
            final var existingRoundIds = existingRounds.stream()
                    .map(Round::getId)
                    .toList();
            if (matchStore.existsResultByRoundIds(existingRoundIds)) {
                throw new ConflictException("Impossible de régénérer le calendrier : des résultats ont déjà été saisis");
            }
            matchStore.deleteAllByRoundIds(existingRoundIds);
            roundStore.deleteAllByTournamentId(TournamentId.of(tournamentId));
        }
        final var allTeams = teamStore.findAllByTournamentId(TournamentId.of(tournamentId));
        final var teamsByGroupId = allTeams.stream()
                .collect(Collectors.groupingBy(t -> t.getGroupId().value()));
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var matchesByGroupId = new LinkedHashMap<UUID, List<MatchPair>>();
        for (final var group : groups) {
            final var groupTeams = teamsByGroupId.getOrDefault(group.getId().value(), List.of());
            final var teamIds = groupTeams.stream().map(Team::getId).toList();
            matchesByGroupId.put(group.getId().value(), generateGroupMatches(group.getId(), teamIds));
        }
        final var fieldQueues = buildFieldQueues(groups, matchesByGroupId, tournament.getNumberOfFields());
        final var totalRounds = fieldQueues.values().stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        final var firstRoundStart = LocalDateTime.of(tournament.getDate(), request.getStartTime());
        final var rounds = new ArrayList<Round>();
        final var matches = new ArrayList<Match>();
        for (int r = 0; r < totalRounds; r++) {
            final var roundStart = firstRoundStart.plusMinutes(
                    (long) r * (request.getMatchDurationMinutes() + request.getBreakDurationMinutes()));
            final var roundId = RoundId.of(UUID.randomUUID());
            rounds.add(Round.builder()
                    .id(roundId)
                    .tournamentId(TournamentId.of(tournamentId))
                    .number(r + 1)
                    .startTime(roundStart)
                    .build());
            for (final var entry : fieldQueues.entrySet()) {
                final var field = entry.getKey();
                final var queue = entry.getValue();
                if (r < queue.size()) {
                    final var pair = queue.get(r);
                    matches.add(Match.builder()
                            .id(MatchId.of(UUID.randomUUID()))
                            .roundId(roundId)
                            .field(field)
                            .groupId(pair.groupId())
                            .team1Id(pair.team1Id())
                            .team2Id(pair.team2Id())
                            .build());
                }
            }
        }
        roundStore.saveAll(rounds);
        matchStore.saveAll(matches);
        return buildScheduleOverview(rounds, matches, groupById, teamById);
    }

    public ScheduleOverview findByTournamentId(final UUID tournamentId) {
        final var rounds = roundStore.findAllByTournamentId(TournamentId.of(tournamentId));
        if (rounds.isEmpty()) {
            return ScheduleOverview.empty();
        }

        final var roundIds = rounds.stream().map(Round::getId).toList();
        final var matches = matchStore.findAllByRoundIds(roundIds);
        final var groups = groupStore.findAllByTournamentId(TournamentId.of(tournamentId));
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var allTeams = teamStore.findAllByTournamentId(TournamentId.of(tournamentId));
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return buildScheduleOverview(rounds, matches, groupById, teamById);
    }

    private static ScheduleOverview buildScheduleOverview(
            final List<Round> rounds,
            final List<Match> matches,
            final Map<UUID, Group> groupById,
            final Map<UUID, Team> teamById) {
        final var matchesByRoundId = matches.stream()
                .collect(Collectors.groupingBy(m -> m.getRoundId().value()));
        final var roundOverviews = rounds.stream()
                .map(r -> {
                    final var roundMatches = matchesByRoundId.getOrDefault(r.getId().value(), List.of());
                    final var matchOverviews = roundMatches.stream()
                            .map(m -> toMatchOverview(groupById, teamById, m))
                            .toList();
                    return toScheduleOverview(r, matchOverviews);
                })
                .toList();
        return ScheduleOverview.builder()
                .totalRounds(rounds.size())
                .totalMatches(matches.size())
                .rounds(roundOverviews)
                .build();
    }

    private static RoundOverview toScheduleOverview(final Round round, final List<MatchOverview> matches) {
        return RoundOverview.builder()
                .id(round.getId())
                .number(round.getNumber())
                .startTime(round.getStartTime())
                .matches(matches)
                .build();
    }

    private static MatchOverview toMatchOverview(
            final Map<UUID, Group> groupById,
            final Map<UUID, Team> teamById,
            final Match match) {
        return MatchOverview.builder()
                .id(match.getId())
                .field(match.getField())
                .group(toGroupRef(match.getGroupId(), groupById.get(match.getGroupId().value()).getName()))
                .team1(toTeamRef(match.getTeam1Id(), teamById.get(match.getTeam1Id().value()).getName()))
                .team2(toTeamRef(match.getTeam2Id(), teamById.get(match.getTeam2Id().value()).getName()))
                .result(match.getResult())
                .build();
    }

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

    private static Map<Integer, List<MatchPair>> buildFieldQueues(
            final List<Group> groups,
            final Map<UUID, List<MatchPair>> matchesByGroupId,
            final int numFields) {
        final var groupMatchesByField = new LinkedHashMap<Integer, List<List<MatchPair>>>();
        for (int f = 1; f <= numFields; f++) {
            groupMatchesByField.put(f, new ArrayList<>());
        }
        for (int i = 0; i < groups.size(); i++) {
            final var field = (i % numFields) + 1;
            final var groupMatches = matchesByGroupId.getOrDefault(groups.get(i).getId().value(), List.of());
            groupMatchesByField.get(field).add(new ArrayList<>(groupMatches));
        }
        final var fieldQueues = new LinkedHashMap<Integer, List<MatchPair>>();
        for (int f = 1; f <= numFields; f++) {
            final var queue = new ArrayList<MatchPair>();
            final var groupLists = groupMatchesByField.get(f);
            final var indices = new int[groupLists.size()];
            while (true) {
                var added = false;
                for (int g = 0; g < groupLists.size(); g++) {
                    if (indices[g] < groupLists.get(g).size()) {
                        queue.add(groupLists.get(g).get(indices[g]));
                        indices[g]++;
                        added = true;
                    }
                }
                if (!added) break;
            }
            fieldQueues.put(f, queue);
        }
        return fieldQueues;
    }
}
