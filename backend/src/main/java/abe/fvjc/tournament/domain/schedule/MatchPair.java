package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.group.GroupId;
import abe.fvjc.tournament.domain.team.TeamId;

public record MatchPair(GroupId groupId, TeamId team1Id, TeamId team2Id) {
}
