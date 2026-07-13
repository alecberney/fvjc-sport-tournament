package abe.fvjc.tournament.domain.team;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.organisation.OrganisationId;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class Team {
    TeamId id;
    String name;
    boolean paid;
    OrganisationId organisationId;
    TournamentId tournamentId;
    @Builder.Default GroupId groupId = GroupId.empty();
}
