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
import java.util.LinkedHashMap;
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
    private final RoundRobinService roundRobinService;
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
        final var teamById = allTeams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        final var groupById = groups.stream()
                .collect(Collectors.toMap(g -> g.getId().value(), g -> g));
        final var teamIdsByGroupId = toTeamIdsByGroupId(groups, allTeams);
        final var packedRounds = roundRobinService.planRounds(teamIdsByGroupId, tournament.getNumberOfFields());
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

    // Keeps the groups' order so the generated schedule is deterministic for a given input.
    private static Map<GroupId, List<TeamId>> toTeamIdsByGroupId(final List<Group> groups, final List<Team> teams) {
        final var teamIdsByGroupId = new LinkedHashMap<GroupId, List<TeamId>>();
        groups.forEach(group -> teamIdsByGroupId.put(group.getId(), new ArrayList<>()));
        teams.stream()
                .filter(team -> teamIdsByGroupId.containsKey(team.getGroupId()))
                .forEach(team -> teamIdsByGroupId.get(team.getGroupId()).add(team.getId()));
        return teamIdsByGroupId;
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
}
