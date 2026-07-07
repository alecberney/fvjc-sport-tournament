package abe.fvjc.tournament.api.schedule;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class RoundDto {
    UUID id;
    int number;
    LocalDateTime startTime;
    List<MatchDto> matches;
}
