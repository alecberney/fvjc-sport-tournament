package abe.fvjc.tournament.organisation.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrganisationStore {
    Organisation save(Organisation organisation);
    Optional<Organisation> findById(UUID id);
    void deleteById(UUID id);
}
