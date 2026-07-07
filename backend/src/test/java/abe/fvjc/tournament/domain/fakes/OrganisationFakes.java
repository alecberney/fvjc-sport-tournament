package abe.fvjc.tournament.domain.fakes;

import abe.fvjc.tournament.domain.organisation.Organisation;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.experimental.UtilityClass;

import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomOrganisationId;
import static abe.fvjc.tournament.domain.fakes.IdGenerator.randomTournamentId;
import static abe.fvjc.tournament.domain.fakes.PersonFakes.buildJeanDupont;

@UtilityClass
public class OrganisationFakes {

    public static Organisation buildOrganisation() {
        return Organisation.builder()
                .id(randomOrganisationId())
                .responsible(buildJeanDupont())
                .tournamentId(randomTournamentId())
                .build();
    }

    public static Organisation buildOrganisation(final TournamentId tournamentId) {
        return Organisation.builder()
                .id(randomOrganisationId())
                .responsible(buildJeanDupont())
                .tournamentId(tournamentId)
                .build();
    }
}
