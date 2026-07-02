package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Person;
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
