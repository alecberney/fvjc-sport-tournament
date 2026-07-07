package abe.fvjc.tournament.domain.schedule;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class ScheduleGenerateRequest {
    LocalTime startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
