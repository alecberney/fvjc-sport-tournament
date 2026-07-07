package abe.fvjc.tournament.domain.organisation;

import java.util.Optional;

public interface OrganisationStore {
    Organisation save(Organisation organisation);

    Optional<Organisation> findById(OrganisationId id);

    void deleteById(OrganisationId id);
}
