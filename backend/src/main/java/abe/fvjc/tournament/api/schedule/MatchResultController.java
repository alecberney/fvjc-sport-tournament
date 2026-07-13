package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.schedule.MatchId;
import abe.fvjc.tournament.domain.schedule.MatchResultService;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static abe.fvjc.tournament.api.schedule.MatchResultApiMapper.toMatchResultResponseDto;
import static abe.fvjc.tournament.api.schedule.MatchResultApiMapper.toSubmitMatchResultRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/matches")
@RequiredArgsConstructor
class MatchResultController {
    private final GroupRankingService groupRankingService;
    private final MatchResultService matchResultService;

    @PutMapping("/{matchId}/result")
    public MatchResultResponseDto submitResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid SubmitMatchResultRequestDto requestDto) {
        final var request = toSubmitMatchResultRequest(requestDto);
        final var matchOverview = matchResultService.submitResult(TournamentId.of(tournamentId), MatchId.of(matchId), request);
        final var ranking = groupRankingService.computeGroupRanking(TournamentId.of(tournamentId), matchOverview.getGroupId());
        return toMatchResultResponseDto(matchOverview, ranking);
    }
}
