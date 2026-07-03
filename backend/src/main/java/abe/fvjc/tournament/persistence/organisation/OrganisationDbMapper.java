package abe.fvjc.tournament.organisation.persistence;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class OrganisationDbMapper {

    static Organisation toOrganisation(final OrganisationEntity entity) {
        final var responsible = Person.builder()
            .firstName(entity.getResponsibleFirstName())
            .lastName(entity.getResponsibleLastName())
            .build();
        return Organisation.builder()
            .id(OrganisationId.of(entity.getId()))
            .responsible(responsible)
            .tournamentId(TournamentId.of(entity.getTournamentId()))
            .build();
    }

    static OrganisationEntity toOrganisationEntity(final Organisation organisation) {
        final var entity = new OrganisationEntity();
        final var id = organisation.getId().isEmpty()
                ? UUID.randomUUID()
                : organisation.getId().value();
        entity.setId(id);
        entity.setResponsibleFirstName(organisation.getResponsible().getFirstName());
        entity.setResponsibleLastName(organisation.getResponsible().getLastName());
        entity.setTournamentId(organisation.getTournamentId().value());
        return entity;
    }
}
