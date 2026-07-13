package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.organisation.Person;
import abe.fvjc.tournament.domain.tournament.TournamentId;
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
