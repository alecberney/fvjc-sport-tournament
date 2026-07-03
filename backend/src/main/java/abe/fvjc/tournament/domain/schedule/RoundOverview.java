package abe.fvjc.tournament.schedule.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class RoundOverview {
    RoundId id;
    int number;
    LocalDateTime startTime;
    List<MatchOverview> matches;
}
