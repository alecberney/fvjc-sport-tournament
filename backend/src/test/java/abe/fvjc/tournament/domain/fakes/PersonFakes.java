package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.organisation.Person;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersonFakes {
    public static Person buildJeanDupont() {
        return Person.builder()
                .firstName("Jean")
                .lastName("Dupont")
                .build();
    }

    public static Person buildMarieMartin() {
        return Person.builder()
                .firstName("Marie")
                .lastName("Martin")
                .build();
    }
}
