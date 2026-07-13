package abe.fvjc.tournament.domain.organisation;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildOrganisationForTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationSearchServiceTest {

    @Mock
    private OrganisationStore organisationStore;

    @InjectMocks
    private OrganisationSearchService organisationSearchService;

    @Test
    void findByIdWhenExistsShouldReturnOrganisation() {
        final var organisation = buildOrganisationForTournament(TournamentId.of(UUID.randomUUID()));
        final var id = organisation.getId().value();

        when(organisationStore.findById(OrganisationId.of(id))).thenReturn(Optional.of(organisation));

        final var organisationFound = organisationSearchService.findById(OrganisationId.of(id));

        verify(organisationStore).findById(OrganisationId.of(id));

        assertEquals(organisation, organisationFound);
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();

        when(organisationStore.findById(OrganisationId.of(id))).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> organisationSearchService.findById(OrganisationId.of(id)));

        verify(organisationStore).findById(OrganisationId.of(id));

        assertEquals("Organisation not found with id: " + id, exception.getMessage());
    }
}
