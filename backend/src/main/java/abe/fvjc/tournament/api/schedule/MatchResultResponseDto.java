package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class MatchResultResponseDto {
    MatchDto match;
    GroupRankingDto ranking;
}
