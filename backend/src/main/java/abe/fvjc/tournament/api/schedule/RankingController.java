package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.RankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.RankingApiMapper.toGroupRankingDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}")
@RequiredArgsConstructor
class RankingController {
    private final RankingService rankingService;

    @GetMapping("/groups/{groupId}/ranking")
    public GroupRankingDto getGroupRanking(
            @PathVariable final UUID tournamentId,
            @PathVariable final UUID groupId) {
        final var ranking = rankingService.computeGroupRanking(tournamentId, groupId);
        return toGroupRankingDto(ranking);
    }

    @GetMapping("/groups/rankings")
    public List<GroupRankingDto> getAllGroupRankings(
            @PathVariable final UUID tournamentId,
            @ModelAttribute final GroupRankingSearchRequest request) {
        return rankingService.computeAllGroupRankings(tournamentId, request.getGroups()).stream()
                .map(ranking -> toGroupRankingDto(ranking))
                .toList();
    }
}
