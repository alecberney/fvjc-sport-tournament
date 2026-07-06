package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
public class GroupRankingSearchRequest {
    List<String> groups;
}
