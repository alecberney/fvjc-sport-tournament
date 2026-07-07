package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.api.group.GroupRankingDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

// TODO useful?
@Value
@Builder
@Jacksonized
public class MatchResultResponseDto {
    MatchDto match;
    GroupRankingDto ranking;
}
