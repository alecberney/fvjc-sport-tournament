package abe.fvjc.tournament.domain.organisation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Person {
    String firstName;
    String lastName;
}
