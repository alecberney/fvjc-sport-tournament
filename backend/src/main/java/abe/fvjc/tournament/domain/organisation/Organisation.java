package abe.fvjc.tournament.organisation.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Organisation {
    OrganisationId id;
    Person responsible;
    TournamentId tournamentId;
}
