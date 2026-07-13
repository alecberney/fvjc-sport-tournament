package abe.fvjc.tournament.api.team;

import abe.fvjc.tournament.domain.team.TeamId;
import abe.fvjc.tournament.domain.team.TeamService;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamDto;
import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamDtos;
import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamRegisterRequest;
import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamUpdateRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/teams")
@RequiredArgsConstructor
class TeamController {
    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<TeamDto> create(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid TeamRegisterRequestDto requestDto) {
        final var request = toTeamRegisterRequest(requestDto);
        final var teams = teamService.registerTeams(TournamentId.of(tournamentId), request);
        return toTeamDtos(teams);
    }

    @GetMapping
    public List<TeamDto> getAll(@PathVariable UUID tournamentId) {
        final var teams = teamService.findAllByTournamentId(TournamentId.of(tournamentId));
        return toTeamDtos(teams);
    }

    @PutMapping("/{teamId}")
    public TeamDto update(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamUpdateRequestDto requestDto) {
        final var request = toTeamUpdateRequest(requestDto);
        final var team = teamService.updateTeam(TournamentId.of(tournamentId), TeamId.of(teamId), request);
        return toTeamDto(team);
    }

    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID tournamentId, @PathVariable UUID teamId) {
        teamService.deleteTeam(TournamentId.of(tournamentId), TeamId.of(teamId));
    }

    @PatchMapping("/{teamId}/paid")
    public TeamDto updatePaid(
            @PathVariable UUID tournamentId,
            @PathVariable UUID teamId,
            @RequestBody @Valid TeamPaidRequestDto requestDto) {
        final var team = teamService.markPaid(TeamId.of(teamId), requestDto.getPaid());
        return toTeamDto(team);
    }
}
