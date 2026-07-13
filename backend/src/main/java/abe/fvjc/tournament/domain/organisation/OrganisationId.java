package abe.fvjc.tournament.domain.organisation;

import java.util.UUID;

public record OrganisationId(UUID value) {

    public static OrganisationId of(final UUID value) {
        return new OrganisationId(value);
    }

    public static OrganisationId empty() {
        return new OrganisationId(null);
    }

    public boolean isEmpty() {
        return value == null;
    }
}
