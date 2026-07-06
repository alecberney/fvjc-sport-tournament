package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import abe.fvjc.tournament.schedule.domain.ResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.ResultApiMapper.toMatchResultResponseDto;
import static abe.fvjc.tournament.schedule.api.ResultApiMapper.toSubmitMatchResultRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class ResultController {
    private final RankingService rankingService;
    private final ResultService resultService;

    @PutMapping("/matches/{matchId}/result")
    public MatchResultResponseDto submitResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid SubmitMatchResultRequestDto request) {
        final var matchOverview = resultService.submitResult(tournamentId, matchId, toSubmitMatchResultRequest(request));
        final var ranking = rankingService.computeGroupRanking(tournamentId, matchOverview.getGroupId().value());
        return toMatchResultResponseDto(matchOverview, ranking);
    }
}
