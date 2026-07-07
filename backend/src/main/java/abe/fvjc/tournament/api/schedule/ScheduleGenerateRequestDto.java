package abe.fvjc.tournament.api.schedule;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalTime;

@Value
@Builder
@Jacksonized
public class ScheduleGenerateRequestDto {
    @NotNull
    LocalTime startTime;

    @NotNull
    @Min(1)
    Integer matchDurationMinutes;

    @NotNull
    @Min(0)
    Integer breakDurationMinutes;
}
