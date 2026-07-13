package abe.fvjc.tournament.api.bracket;

import abe.fvjc.tournament.domain.bracket.BracketMatchId;
import abe.fvjc.tournament.domain.bracket.BracketService;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static abe.fvjc.tournament.api.bracket.BracketApiMapper.toBracketGenerateRequest;
import static abe.fvjc.tournament.api.bracket.BracketApiMapper.toBracketMatchDto;
import static abe.fvjc.tournament.api.bracket.BracketApiMapper.toBracketMatchResultRequest;
import static abe.fvjc.tournament.api.bracket.BracketApiMapper.toBracketRoundDtos;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/bracket")
@RequiredArgsConstructor
class BracketController {
    private final BracketService bracketService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<BracketRoundDto> generate(
            @PathVariable final UUID tournamentId,
            @RequestBody @Valid final BracketGenerateRequestDto requestDto) {
        final var request = toBracketGenerateRequest(requestDto);
        final var rounds = bracketService.generate(TournamentId.of(tournamentId), request);
        return toBracketRoundDtos(rounds);
    }

    @GetMapping
    public List<BracketRoundDto> getAll(@PathVariable final UUID tournamentId) {
        final var rounds = bracketService.findAll(TournamentId.of(tournamentId));
        return toBracketRoundDtos(rounds);
    }

    @PutMapping("/matches/{matchId}/result")
    public BracketMatchDto enterResult(
            @PathVariable final UUID tournamentId,
            @PathVariable final UUID matchId,
            @RequestBody @Valid final BracketMatchResultRequestDto requestDto) {
        final var request = toBracketMatchResultRequest(requestDto);
        final var match = bracketService.enterResult(BracketMatchId.of(matchId), request);
        return toBracketMatchDto(match);
    }
}
