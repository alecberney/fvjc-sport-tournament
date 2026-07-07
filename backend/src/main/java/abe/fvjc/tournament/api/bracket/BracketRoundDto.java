package abe.fvjc.tournament.api.bracket;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketRoundDto {
    UUID id;
    int number;
    String name;
    LocalDateTime startTime;
    List<BracketMatchDto> matches;
}
