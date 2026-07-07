package abe.fvjc.tournament.domain.organisation;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganisationService {
    private final OrganisationStore organisationStore;

    public Organisation create(final Person responsible, final TournamentId tournamentId) {
        final var organisation = buildOrganisation(responsible, tournamentId);
        return organisationStore.save(organisation);
    }

    public Organisation updateResponsible(final Person responsible, final OrganisationId organisationId) {
        final var organisation = organisationStore.findById(organisationId)
            .orElseThrow(() -> new NotFoundException("Organisation", organisationId.value()));
        final var organisationUpdated = organisation.withResponsible(responsible);
        return organisationStore.save(organisationUpdated);
    }

    private static Organisation buildOrganisation(final Person responsible, final TournamentId tournamentId) {
        return Organisation.builder()
                .id(generateOrganisationId())
                .responsible(responsible)
                .tournamentId(tournamentId)
                .build();
    }

    private static OrganisationId generateOrganisationId() {
        return OrganisationId.of(UUID.randomUUID());
    }
}
