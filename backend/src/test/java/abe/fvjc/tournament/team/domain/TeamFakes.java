package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.Organisation;
import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class TeamFakes {

    public static Organisation buildOrganisationForTournament(final TournamentId tournamentId) {
        return Organisation.builder()
            .id(IdGenerator.organisationId())
            .responsible(Person.builder().firstName("Jean").lastName("Dupont").build())
            .tournamentId(tournamentId)
            .build();
    }

    public static Team buildTeam(final OrganisationId organisationId, final TournamentId tournamentId) {
        return Team.builder()
            .id(IdGenerator.teamId())
            .name("Les Aigles")
            .paid(false)
            .organisationId(organisationId)
            .tournamentId(tournamentId)
            .build();
    }

    public static TeamRegisterRequest buildRegisterRequest() {
        return TeamRegisterRequest.builder()
            .name("Les Aigles")
            .responsible(Person.builder().firstName("Jean").lastName("Dupont").build())
            .count(1)
            .paid(List.of(false))
            .build();
    }

    public static TeamRegisterRequest buildRegisterRequestWithCount(final int count) {
        return TeamRegisterRequest.builder()
            .name("Les Aigles")
            .responsible(Person.builder().firstName("Jean").lastName("Dupont").build())
            .count(count)
            .paid(Collections.nCopies(count, false))
            .build();
    }

    public static TeamUpdateRequest buildUpdateRequest() {
        return TeamUpdateRequest.builder()
            .name("Les Aigles Modifié")
            .responsible(Person.builder().firstName("Marie").lastName("Martin").build())
            .paid(true)
            .build();
    }
}
