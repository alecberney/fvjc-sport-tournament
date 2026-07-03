package abe.fvjc.tournament.group.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupTeamDto {
    UUID id;
    String name;
    UUID organisationId;
}
