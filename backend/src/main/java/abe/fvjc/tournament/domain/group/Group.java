package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
@With
public class Group {
    GroupId id;
    String name;
    TournamentId tournamentId;
}
