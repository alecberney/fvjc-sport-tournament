package abe.fvjc.tournament.api.team;

import abe.fvjc.tournament.domain.organisation.Person;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PersonApiMapper {
    static PersonDto toPersonDto(final Person person) {
        if (person == null) {
            return null;
        }
        return PersonDto.builder()
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .build();
    }
}
