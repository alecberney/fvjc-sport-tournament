package abe.fvjc.tournament.domain.team;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TeamRef {
    TeamId id;
    String name;

    public static TeamRef toTeamRef(final TeamId id, final String name) {
        return TeamRef.builder()
                .id(id)
                .name(name)
                .build();
    }
}
