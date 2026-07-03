package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ScheduleOverview {
    int totalRounds;
    int totalMatches;
    List<RoundOverview> rounds;
}
