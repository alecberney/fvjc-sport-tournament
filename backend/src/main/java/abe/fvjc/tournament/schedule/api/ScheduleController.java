package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static abe.fvjc.tournament.schedule.api.ScheduleApiMapper.*;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/schedule")
@RequiredArgsConstructor
class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleDto generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid ScheduleGenerateRequestDto request) {
        return toScheduleDto(scheduleService.generate(tournamentId, toScheduleGenerateRequest(request)));
    }

    @GetMapping
    public ScheduleDto getSchedule(@PathVariable UUID tournamentId) {
        return toScheduleDto(scheduleService.findByTournamentId(tournamentId));
    }
}
