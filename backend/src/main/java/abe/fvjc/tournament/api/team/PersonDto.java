package abe.fvjc.tournament.api.team;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PersonDto {
    String firstName;
    String lastName;
}
