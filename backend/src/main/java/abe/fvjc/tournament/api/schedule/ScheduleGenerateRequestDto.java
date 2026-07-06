package abe.fvjc.tournament.schedule.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ScheduleGenerateRequestDto {
    @NotNull String startTime;
    @NotNull @Min(1) Integer matchDurationMinutes;
    @NotNull @Min(0) Integer breakDurationMinutes;
}
