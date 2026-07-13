package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import abe.fvjc.tournament.domain.common.problem.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static abe.fvjc.tournament.domain.fakes.TeamFakes.buildTeam;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamSearchServiceTest {

    @Mock
    private TeamStore teamStore;

    @InjectMocks
    private TeamSearchService teamSearchService;

    @Test
    void findByIdWhenExistsShouldReturnTeam() {
        final var team = buildTeam(OrganisationId.of(UUID.randomUUID()), TournamentId.of(UUID.randomUUID()));
        final var id = team.getId().value();

        when(teamStore.findById(TeamId.of(id))).thenReturn(Optional.of(team));

        final var teamFound = teamSearchService.findById(TeamId.of(id));

        verify(teamStore).findById(TeamId.of(id));

        assertEquals(team, teamFound);
    }

    @Test
    void findByIdWhenNotFoundShouldThrowNotFoundException() {
        final var id = UUID.randomUUID();

        when(teamStore.findById(TeamId.of(id))).thenReturn(Optional.empty());

        final var exception = assertThrows(NotFoundException.class, () -> teamSearchService.findById(TeamId.of(id)));

        verify(teamStore).findById(TeamId.of(id));

        assertEquals("Team not found with id: " + id, exception.getMessage());
    }
}
