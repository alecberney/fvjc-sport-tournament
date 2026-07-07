package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@With
@Builder
public class Group {
    GroupId id;
    String name;
    TournamentId tournamentId;
}
