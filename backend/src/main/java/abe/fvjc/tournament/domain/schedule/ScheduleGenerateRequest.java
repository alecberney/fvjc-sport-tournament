package abe.fvjc.tournament.domain.schedule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScheduleGenerateRequest {
    String startTime;
    Integer matchDurationMinutes;
    Integer breakDurationMinutes;
}
