package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.group.domain.GroupStore;
import abe.fvjc.tournament.shared.exception.ConflictException;
import abe.fvjc.tournament.shared.exception.NotFoundException;
import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.team.domain.TeamStore;

import static abe.fvjc.tournament.schedule.domain.TeamRef.toTeamRef;
import abe.fvjc.tournament.tournament.domain.TournamentStatus;
import abe.fvjc.tournament.tournament.domain.TournamentStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.schedule.domain.ResultValidator.validateSubmitMatchResultRequest;

@Service
@RequiredArgsConstructor
public class ResultService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;
    private final TournamentStore tournamentStore;

    public MatchOverview submitResult(final UUID tournamentId, final UUID matchId,
                                      final SubmitMatchResultRequest request) {
        final var tournament = tournamentStore.findById(tournamentId)
                .orElseThrow(() -> new NotFoundException("Tournament", tournamentId));
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new ConflictException("Les résultats ne peuvent être saisis que pour un tournoi en cours");
        }
        final var match = matchStore.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match", matchId));
        validateSubmitMatchResultRequest(request);
        final var result = MatchResult.builder()
                .score1(request.getScore1())
                .score2(request.getScore2())
                .build();
        final var savedMatch = matchStore.save(match.withResult(result));
        return buildMatchOverview(savedMatch, tournamentId);
    }

    private MatchOverview buildMatchOverview(final Match match, final UUID tournamentId) {
        final var groups = groupStore.findAllByTournamentId(tournamentId);
        final var groupName = groups.stream()
                .filter(g -> g.getId().value().equals(match.getGroupId().value()))
                .map(abe.fvjc.tournament.group.domain.Group::getName)
                .findFirst()
                .orElse("");
        final var teams = teamStore.findAllByGroupId(match.getGroupId().value());
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
