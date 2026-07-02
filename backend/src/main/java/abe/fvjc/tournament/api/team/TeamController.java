package abe.fvjc.tournament.team.api;

import abe.fvjc.tournament.team.domain.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.team.api.TeamApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/teams")
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeamDto> create(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid TeamRegisterRequestDto request) {
        return teamService.registerTeams(tournamentId, toTeamRegisterRequest(request)).stream()
                .map(view -> toTeamDto(view))
                .toList();
    }

    @GetMapping
    public List<TeamDto> getAll(@PathVariable UUID tournamentId) {
        return teamService.findAllByTournamentId(tournamentId).stream()
                .map(view -> toTeamDto(view))
                .toList();
    }

    @PutMapping("/{teamId}")
    public TeamDto update(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamUpdateRequestDto request) {
        return toTeamDto(teamService.updateTeam(tournamentId, teamId, toTeamUpdateRequest(request)));
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tournamentId, @PathVariable UUID teamId) {
        teamService.deleteTeam(tournamentId, teamId);
    }

    @PatchMapping("/{teamId}/paid")
    public TeamDto updatePaid(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamPaidRequestDto request) {
        return toTeamDto(teamService.markPaid(teamId, request.getPaid()));
    }
}
