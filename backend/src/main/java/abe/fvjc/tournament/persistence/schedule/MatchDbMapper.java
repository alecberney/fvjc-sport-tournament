package abe.fvjc.tournament.schedule.persistence;

import abe.fvjc.tournament.group.domain.GroupId;
import abe.fvjc.tournament.schedule.domain.Match;
import abe.fvjc.tournament.schedule.domain.MatchId;
import abe.fvjc.tournament.schedule.domain.RoundId;
import abe.fvjc.tournament.team.domain.TeamId;
import lombok.experimental.UtilityClass;

@UtilityClass
class MatchDbMapper {

    static Match toMatch(final MatchEntity entity) {
        return Match.builder()
                .id(MatchId.of(entity.getId()))
                .roundId(RoundId.of(entity.getRoundId()))
                .field(entity.getField())
                .groupId(GroupId.of(entity.getGroupId()))
                .team1Id(TeamId.of(entity.getTeam1Id()))
                .team2Id(TeamId.of(entity.getTeam2Id()))
                .build();
    }

    static MatchEntity toMatchEntity(final Match match) {
        final var entity = new MatchEntity();
        entity.setId(match.getId().value());
        entity.setRoundId(match.getRoundId().value());
        entity.setField(match.getField());
        entity.setGroupId(match.getGroupId().value());
        entity.setTeam1Id(match.getTeam1Id().value());
        entity.setTeam2Id(match.getTeam2Id().value());
        return entity;
    }
}
