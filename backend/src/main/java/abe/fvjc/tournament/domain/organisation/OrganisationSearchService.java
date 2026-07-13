package abe.fvjc.tournament.domain.organisation;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganisationSearchService {
    private final OrganisationStore organisationStore;

    public Organisation findById(final OrganisationId organisationId) {
        return organisationStore.findById(organisationId)
                .orElseThrow(() -> new NotFoundException("Organisation", organisationId.value()));
    }
}
