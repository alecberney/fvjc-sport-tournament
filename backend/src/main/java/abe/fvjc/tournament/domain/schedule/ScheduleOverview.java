package abe.fvjc.tournament.domain.schedule;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ScheduleOverview {
    int totalRounds;
    int totalMatches;
    List<RoundOverview> rounds;

    public static ScheduleOverview empty() {
        return ScheduleOverview.builder()
                .totalRounds(0)
                .totalMatches(0)
                .rounds(List.of())
                .build();
    }
}
