package abe.fvjc.tournament.organisation.persistence;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.organisation.persistence.OrganisationDbMapper.toOrganisation;
import static abe.fvjc.tournament.organisation.persistence.OrganisationDbMapper.toOrganisationEntity;

@Repository
@RequiredArgsConstructor
class JpaOrganisationStore implements OrganisationStore {
    private final OrganisationRepository organisationRepository;

    @Override
    @Transactional
    public Organisation save(final Organisation organisation) {
        final var entity = toOrganisationEntity(organisation);
        final var savedEntity = organisationRepository.save(entity);
        return toOrganisation(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organisation> findById(final UUID id) {
        return organisationRepository.findById(id)
                .map(OrganisationDbMapper::toOrganisation);
    }

    @Override
    @Transactional
    public void deleteById(final UUID id) {
        organisationRepository.deleteById(id);
    }
}
