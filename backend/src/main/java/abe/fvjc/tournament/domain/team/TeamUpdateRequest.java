package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Person;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamUpdateRequest {
    String name;
    Person responsible;
    boolean paid;
}
