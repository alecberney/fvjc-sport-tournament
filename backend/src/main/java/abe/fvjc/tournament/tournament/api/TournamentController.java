package abe.fvjc.tournament.tournament.api;

import abe.fvjc.tournament.tournament.domain.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
class TournamentController {

    private final TournamentService tournamentService;

    @GetMapping
    public List<TournamentDto> getAll() {
        return null;
    }

    @GetMapping("/{id}")
    public TournamentDto getById(@PathVariable UUID id) {
        return null;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDto create(@RequestBody @Valid TournamentCreateRequestDto request) {
        return null;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
    }
}
