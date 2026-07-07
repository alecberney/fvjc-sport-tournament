package abe.fvjc.tournament.api.schedule;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class ScheduleDto {
    int totalRounds;
    int totalMatches;
    List<RoundDto> rounds;
}
