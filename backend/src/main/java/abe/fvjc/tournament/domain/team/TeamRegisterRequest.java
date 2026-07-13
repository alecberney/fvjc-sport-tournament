package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.Person;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TeamRegisterRequest {
    String name;
    Person responsible;
    int count;
    List<Boolean> paid;
}
