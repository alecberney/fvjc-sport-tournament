package abe.fvjc.tournament.group.api;

import abe.fvjc.tournament.group.domain.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.group.api.GroupApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/groups")
@RequiredArgsConstructor
class GroupController {
    private final GroupService groupService;

    @PostMapping("/distribution")
    public GroupDistributionDto distribution(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto request) {
        final var generateRequest = toGroupGenerateRequest(request);
        return toGroupDistributionDto(groupService.distribution(tournamentId, generateRequest));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<GroupDto> generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupGenerateRequestDto request) {
        return groupService.generate(tournamentId, toGroupGenerateRequest(request))
                .stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    @GetMapping
    public List<GroupDto> getAll(@PathVariable UUID tournamentId) {
        return groupService.findAllByTournamentId(tournamentId)
                .stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    @PostMapping("/swap")
    public List<GroupDto> swap(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid GroupSwapRequestDto request) {
        return groupService.swap(tournamentId, toGroupSwapRequest(request))
                .stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }
}
