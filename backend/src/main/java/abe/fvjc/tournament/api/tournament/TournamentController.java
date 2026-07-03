package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.tournament.api.TournamentApiMapper.toTournamentCreateRequest;
import static abe.fvjc.tournament.tournament.api.TournamentApiMapper.toTournamentDto;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
class TournamentController {
    private final TournamentService tournamentService;

    @GetMapping
    public List<TournamentDto> getAll() {
        return tournamentService.findAll().stream()
                .map(TournamentApiMapper::toTournamentDto)
                .toList();
    }

    @GetMapping("/{id}")
    public TournamentDto getById(@PathVariable UUID id) {
        return toTournamentDto(tournamentService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDto create(@RequestBody @Valid TournamentCreateRequestDto request) {
        return toTournamentDto(tournamentService.create(toTournamentCreateRequest(request)));
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
