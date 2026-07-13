package abe.fvjc.tournament.persistence.organisation;

import abe.fvjc.tournament.domain.organisation.Organisation;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.organisation.OrganisationStore;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static abe.fvjc.tournament.persistence.organisation.OrganisationDbMapper.toOrganisation;
import static abe.fvjc.tournament.persistence.organisation.OrganisationDbMapper.toOrganisationEntity;

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
    public Optional<Organisation> findById(final OrganisationId id) {
        return organisationRepository.findById(id.value())
                .map(OrganisationDbMapper::toOrganisation);
    }

    @Override
    @Transactional
    public void deleteById(final OrganisationId id) {
        organisationRepository.deleteById(id.value());
    }

    @Override
    @Transactional
    public void deleteAllByTournamentId(final TournamentId tournamentId) {
        organisationRepository.deleteByTournamentId(tournamentId.value());
    }
}
