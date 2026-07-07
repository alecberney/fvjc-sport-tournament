package abe.fvjc.tournament.domain.organisation;

import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static abe.fvjc.tournament.domain.fakes.PersonFakes.buildJeanDupont;
import static abe.fvjc.tournament.domain.fakes.OrganisationFakes.buildOrganisation;
import static abe.fvjc.tournament.domain.fakes.PersonFakes.buildMarieMartin;
import static abe.fvjc.tournament.domain.fakes.TournamentFakes.buildTournament;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {
    @Mock
    private OrganisationStore organisationStore;

    @InjectMocks
    private OrganisationService organisationService;

    @Test
    void createWhenValidShouldSaveWithResponsibleAndTournamentId() {
        final var responsible = buildJeanDupont();
        final var tournamentId = buildTournament().getId();

        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());

        final var organisationCreated = organisationService.create(responsible, tournamentId);

        verify(organisationStore).save(any(Organisation.class));

        assertEquals(responsible, organisationCreated.getResponsible());
        assertEquals(tournamentId, organisationCreated.getTournamentId());
        assertFalse(organisationCreated.getId().isEmpty());
    }

    @Test
    void updateResponsibleWhenExistsShouldSaveUpdatedResponsible() {
        final var organisation = buildOrganisation();
        final var organisationId = organisation.getId();
        final var responsible = buildMarieMartin();

        when(organisationStore.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(organisationStore.save(any(Organisation.class))).then(returnsFirstArg());

        final var organisationUpdated = organisationService.updateResponsible(responsible, organisationId);

        verify(organisationStore).findById(organisationId);
        verify(organisationStore).save(any(Organisation.class));

        assertEquals(responsible, organisationUpdated.getResponsible());
    }

    @Test
    void updateResponsibleWhenNotFoundShouldThrowNotFoundException() {
        final var organisationId = buildOrganisation().getId();
        final var responsible = buildJeanDupont();

        when(organisationStore.findById(organisationId)).thenReturn(Optional.empty());

        final var exception = assertThrows(
                NotFoundException.class,
                () -> organisationService.updateResponsible(responsible, organisationId));

        verify(organisationStore).findById(organisationId);

        assertTrue(exception.getMessage().contains("Organisation"));
        assertTrue(exception.getMessage().contains(organisationId.value().toString()));
    }
}
