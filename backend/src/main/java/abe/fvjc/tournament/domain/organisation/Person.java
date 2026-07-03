package abe.fvjc.tournament.organisation.domain;

import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Person {
    String firstName;
    String lastName;
}
