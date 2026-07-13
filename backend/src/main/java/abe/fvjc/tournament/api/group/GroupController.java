package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.domain.group.GroupService;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.api.group.GroupApiMapper.toGroupDistributionDto;
import static abe.fvjc.tournament.api.group.GroupApiMapper.toGroupDtos;
import static abe.fvjc.tournament.api.group.GroupApiMapper.toGroupGenerateRequest;
import static abe.fvjc.tournament.api.group.GroupApiMapper.toGroupSwapRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/groups")
@RequiredArgsConstructor
class GroupController {
    private final GroupService groupService;

    @PostMapping("/distribution")
    public GroupDistributionDto distribution(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto requestDto) {
        final var request = toGroupGenerateRequest(requestDto);
        final var distribution = groupService.distribution(TournamentId.of(tournamentId), request);
        return toGroupDistributionDto(distribution);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GroupDto> generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto requestDto) {
        final var request = toGroupGenerateRequest(requestDto);
        final var groups = groupService.generate(TournamentId.of(tournamentId), request);
        return toGroupDtos(groups);
    }

    @GetMapping
    public List<GroupDto> getAll(@PathVariable UUID tournamentId) {
        final var groups = groupService.findAllByTournamentId(TournamentId.of(tournamentId));
        return toGroupDtos(groups);
    }

    @PostMapping("/swap")
    public List<GroupDto> swap(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupSwapRequestDto requestDto) {
        final var request = toGroupSwapRequest(requestDto);
        final var groups = groupService.swap(TournamentId.of(tournamentId), request);
        return toGroupDtos(groups);
    }
}
