package abe.fvjc.tournament.api.tournament;

import abe.fvjc.tournament.domain.tournament.TournamentSearchService;
import abe.fvjc.tournament.domain.tournament.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.api.tournament.TournamentApiMapper.toTournamentCreateRequest;
import static abe.fvjc.tournament.api.tournament.TournamentApiMapper.toTournamentDto;
import static abe.fvjc.tournament.api.tournament.TournamentApiMapper.toTournamentDtos;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
class TournamentController {
    private final TournamentService tournamentService;
    private final TournamentSearchService tournamentSearchService;

    @GetMapping
    public List<TournamentDto> getAll() {
        final var tournaments = tournamentSearchService.findAll();
        return toTournamentDtos(tournaments);
    }

    @GetMapping("/{id}")
    public TournamentDto getById(@PathVariable UUID id) {
        final var tournament = tournamentSearchService.findById(id);
        return toTournamentDto(tournament);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDto create(@RequestBody @Valid TournamentCreateRequestDto requestDto) {
        final var request = toTournamentCreateRequest(requestDto);
        final var tournament = tournamentService.create(request);
        return toTournamentDto(tournament);
    }

    @PostMapping("/{id}/start")
    public TournamentDto start(@PathVariable UUID id) {
        final var tournament = tournamentService.start(id);
        return toTournamentDto(tournament);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        tournamentService.delete(id);
    }
}
