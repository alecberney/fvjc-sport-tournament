package abe.fvjc.tournament.api.group;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupRankingDto {
    // TODO should be GroupRefDto
    UUID groupId;
    String groupName;
    List<GroupRankingEntryDto> entries;
}
