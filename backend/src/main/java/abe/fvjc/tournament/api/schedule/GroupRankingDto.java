package abe.fvjc.tournament.schedule.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupRankingDto {
    UUID groupId;
    String groupName;
    List<GroupRankingEntryDto> entries;
}
