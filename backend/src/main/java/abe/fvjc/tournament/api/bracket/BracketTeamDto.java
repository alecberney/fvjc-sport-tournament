package abe.fvjc.tournament.api.bracket;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketTeamDto {
    UUID id;
    String name;
}
