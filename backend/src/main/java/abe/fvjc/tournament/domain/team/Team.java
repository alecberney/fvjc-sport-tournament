package abe.fvjc.tournament.team.domain;

import abe.fvjc.tournament.organisation.domain.OrganisationId;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Team {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    TournamentId tournamentId;
}
