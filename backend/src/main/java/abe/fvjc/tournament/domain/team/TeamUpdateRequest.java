package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.Person;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamUpdateRequest {
    String name;
    Person responsible;
    boolean paid;
}
