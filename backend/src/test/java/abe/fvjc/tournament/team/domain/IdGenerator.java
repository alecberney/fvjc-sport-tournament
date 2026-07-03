package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
class IdGenerator {
    static TeamId teamId() {
        return TeamId.of(UUID.randomUUID());
    }

    static OrganisationId organisationId() {
        return OrganisationId.of(UUID.randomUUID());
    }
}
