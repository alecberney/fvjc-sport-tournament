package abe.fvjc.tournament.domain.organisation;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class Organisation {
    OrganisationId id;
    Person responsible;
    TournamentId tournamentId;
}
