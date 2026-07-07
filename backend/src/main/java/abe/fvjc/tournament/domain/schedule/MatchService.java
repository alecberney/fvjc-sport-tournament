package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.Group;
import abe.fvjc.tournament.domain.group.GroupStore;
import abe.fvjc.tournament.domain.common.problem.ConflictException;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.team.TeamStore;

import static abe.fvjc.tournament.domain.group.GroupRef.toGroupRef;
import static abe.fvjc.tournament.domain.team.TeamRef.toTeamRef;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

import static abe.fvjc.tournament.domain.schedule.MatchResultValidator.validateSubmitMatchResultRequest;

@Service
@RequiredArgsConstructor
public class MatchService {
    private final GroupStore groupStore;
    private final MatchStore matchStore;
    private final TeamStore teamStore;
    private final TournamentSearchService tournamentSearchService;

    public MatchOverview submitResult(
            final UUID tournamentId,
            final UUID matchId,
            final SubmitMatchResultRequest request) {
        final var tournament = tournamentSearchService.findById(tournamentId);
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new ConflictException("Les résultats ne peuvent être saisis que pour un tournoi en cours");
        }

        final var match = matchStore.findById(MatchId.of(matchId))
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
        final var groups = groupStore.findAllByTournamentId(TournamentId.of(tournamentId));
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
                .group(toGroupRef(match.getGroupId(), groupName))
                .team1(toTeamRef(teamById.get(match.getTeam1Id().value()).getId(), teamById.get(match.getTeam1Id().value()).getName()))
                .team2(toTeamRef(teamById.get(match.getTeam2Id().value()).getId(), teamById.get(match.getTeam2Id().value()).getName()))
                .result(match.getResult())
                .build();
    }

}
