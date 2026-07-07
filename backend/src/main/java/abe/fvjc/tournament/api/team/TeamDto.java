package abe.fvjc.tournament.api.team;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class TeamDto {
    UUID id;
    String name;
    boolean paid;
    UUID organisationId;
}
