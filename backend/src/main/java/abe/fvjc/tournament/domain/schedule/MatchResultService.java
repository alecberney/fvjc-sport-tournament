package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.team.TeamStore;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import static abe.fvjc.tournament.domain.schedule.ResultValidator.validateSubmitMatchResultRequest;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;

@Service
@RequiredArgsConstructor
public class MatchResultService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;
    private final MatchSearchService matchSearchService;
    private final TournamentSearchService tournamentSearchService;

    public MatchOverview submitResult(
            final TournamentId tournamentId,
            final MatchId matchId,
            final SubmitMatchResultRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new ConflictException("Les résultats ne peuvent être saisis que pour un tournoi en cours");
        }
        final var match = matchSearchService.findById(matchId);
        validateSubmitMatchResultRequest(request);
        final var result = buildMatchResult(request);
        final var savedMatch = matchStore.save(match.withResult(result));
        return toMatchOverview(savedMatch, tournamentId);
    }

    private static MatchResult buildMatchResult(final SubmitMatchResultRequest request) {
        return MatchResult.builder()
                .score1(request.getScore1())
                .score2(request.getScore2())
                .build();
    }

    private MatchOverview toMatchOverview(final Match match, final TournamentId tournamentId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupName = groups.stream()
                .filter(g -> g.getId().value().equals(match.getGroupId().value()))
                .map(Group::getName)
                .findFirst()
                .orElse("");
        final var teams = teamStore.findAllByGroupId(match.getGroupId());
        final var teamById = teams.stream()
                .collect(Collectors.toMap(t -> t.getId().value(), t -> t));
        return MatchOverview.builder()
                .id(match.getId())
                .field(match.getField())
                .groupId(match.getGroupId())
                .groupName(groupName)
                .team1(toTeamRef(teamById.get(match.getTeam1Id().value()).getId(), teamById.get(match.getTeam1Id().value()).getName()))
                .team2(toTeamRef(teamById.get(match.getTeam2Id().value()).getId(), teamById.get(match.getTeam2Id().value()).getName()))
                .result(match.getResult())
                .build();
    }
}
