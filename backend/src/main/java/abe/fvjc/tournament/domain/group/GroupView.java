package abe.fvjc.tournament.group.domain;

import abe.fvjc.tournament.team.domain.Team;
import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GroupView {
    GroupId id;
    String name;
    TournamentId tournamentId;
    List<Team> teams;
}
