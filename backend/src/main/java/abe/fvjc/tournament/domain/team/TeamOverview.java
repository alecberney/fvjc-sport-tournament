package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.organisation.domain.Person;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamOverview {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    Person responsible;
    TournamentId tournamentId;
}
