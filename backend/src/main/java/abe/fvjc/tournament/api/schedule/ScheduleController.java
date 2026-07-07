package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.domain.schedule.ScheduleService;
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

import java.util.UUID;

import static abe.fvjc.tournament.api.schedule.ScheduleApiMapper.toScheduleDto;
import static abe.fvjc.tournament.api.schedule.ScheduleApiMapper.toScheduleGenerateRequest;

@RestController
@RequestMapping("/api/tournaments/{tournamentId}/schedule")
@RequiredArgsConstructor
class ScheduleController {
    private final ScheduleService scheduleService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ScheduleDto generate(
            @PathVariable UUID tournamentId,
            @RequestBody @Valid ScheduleGenerateRequestDto requestDto) {
        final var request = toScheduleGenerateRequest(requestDto);
        final var schedule = scheduleService.generate(tournamentId, request);
        return toScheduleDto(schedule);
    }

    @GetMapping
    public ScheduleDto getSchedule(@PathVariable UUID tournamentId) {
        final var schedule = scheduleService.findByTournamentId(tournamentId);
        return toScheduleDto(schedule);
    }
}
