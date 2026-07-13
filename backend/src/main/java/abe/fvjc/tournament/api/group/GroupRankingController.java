package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.group.GroupRankingService;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.api.group.GroupRankingApiMapper.toGroupRankingDto;
import static abe.fvjc.tournament.api.group.GroupRankingApiMapper.toGroupRankingDtos;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/groups")
@RequiredArgsConstructor
class GroupRankingController {
    private final GroupRankingService groupRankingService;

    @GetMapping("/{groupId}/ranking")
    public GroupRankingDto getGroupRanking(
            @PathVariable final UUID tournamentId,
            @PathVariable final UUID groupId) {
        final var ranking = groupRankingService.computeGroupRanking(TournamentId.of(tournamentId), GroupId.of(groupId));
        return toGroupRankingDto(ranking);
    }

    @GetMapping("/rankings")
    public List<GroupRankingDto> getAllGroupRankings(
            @PathVariable final UUID tournamentId,
            @ModelAttribute final GroupRankingSearchRequest request) {
        final var rankings = groupRankingService.computeAllGroupRankings(TournamentId.of(tournamentId), request.getGroups());
        return toGroupRankingDtos(rankings);
    }
}
