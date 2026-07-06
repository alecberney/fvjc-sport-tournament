package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class RankingController {
    private final RankingService rankingService;

    @GetMapping("/groups/{groupId}/ranking")
    public GroupRankingDto getGroupRanking(
            @PathVariable UUID tournamentId,
            @PathVariable UUID groupId) {
        final var ranking = rankingService.computeGroupRanking(tournamentId, groupId);
        return toGroupRankingDto(ranking);
    }
}
