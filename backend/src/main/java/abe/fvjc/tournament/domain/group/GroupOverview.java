package abe.fvjc.tournament.domain.group;

import abe.fvjc.tournament.domain.team.Team;
import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupOverview {
    GroupId id;
    String name;
    TournamentId tournamentId;
    List<Team> teams;
}
