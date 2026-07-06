package abe.fvjc.tournament.bracket.api;

import abe.fvjc.tournament.bracket.domain.BracketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketGenerateRequest;
import static abe.fvjc.tournament.bracket.api.BracketApiMapper.toBracketRoundDto;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/bracket")
@RequiredArgsConstructor
class BracketController {
    private final BracketService bracketService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BracketRoundDto> generate(
            @PathVariable final UUID tournamentId,
            @RequestBody @Valid final BracketGenerateRequestDto request) {
        return bracketService.generate(tournamentId, toBracketGenerateRequest(request)).stream()
                .map(BracketApiMapper::toBracketRoundDto)
                .toList();
    }

    @GetMapping
    public List<BracketRoundDto> getAll(@PathVariable final UUID tournamentId) {
        return bracketService.findAll(tournamentId).stream()
                .map(BracketApiMapper::toBracketRoundDto)
                .toList();
    }
}
