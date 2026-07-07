package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.schedule.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static abe.fvjc.tournament.api.schedule.MatchApiMapper.toMatchResultResponseDto;
import static abe.fvjc.tournament.api.schedule.MatchApiMapper.toSubmitMatchResultRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/matches/{matchId}")
@RequiredArgsConstructor
class MatchController {
    private final GroupRankingService groupRankingService;
    private final MatchService matchService;

    @PutMapping("/result")
    public MatchResultResponseDto submitResult(
            @PathVariable UUID tournamentId,
            @PathVariable UUID matchId,
            @RequestBody @Valid SubmitMatchResultRequestDto requestDto) {
        final var request = toSubmitMatchResultRequest(requestDto);
        final var matchOverview = matchService.submitResult(tournamentId, matchId, request);
        final var ranking = groupRankingService.computeGroupRanking(tournamentId, matchOverview.getGroup().getId().value());
        return toMatchResultResponseDto(matchOverview, ranking);
    }
}
