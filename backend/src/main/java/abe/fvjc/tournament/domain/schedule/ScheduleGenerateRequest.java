package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScheduleGenerateRequest {
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
