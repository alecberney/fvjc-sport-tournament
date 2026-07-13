package abe.fvjc.tournament.domain.organisation;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class Person {
    String firstName;
    String lastName;
}
