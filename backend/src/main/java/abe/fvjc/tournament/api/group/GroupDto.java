package abe.fvjc.tournament.api.group;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupDto {
    UUID id;
    String name;
    List<GroupTeamDto> teams;
}
