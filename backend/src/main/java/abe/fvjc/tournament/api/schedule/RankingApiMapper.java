package abe.fvjc.tournament.schedule.api;

import abe.fvjc.tournament.schedule.domain.GroupRanking;
import abe.fvjc.tournament.schedule.domain.GroupRankingEntry;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RankingApiMapper {

    static GroupRankingDto toGroupRankingDto(final GroupRanking ranking) {
        return GroupRankingDto.builder()
                .groupId(ranking.getGroupId().value())
                .groupName(ranking.getGroupName())
                .entries(ranking.getEntries().stream()
                        .map(RankingApiMapper::toGroupRankingEntryDto)
                        .toList())
                .build();
    }

    private static GroupRankingEntryDto toGroupRankingEntryDto(final GroupRankingEntry entry) {
        return GroupRankingEntryDto.builder()
                .rank(entry.getRank())
                .team(MatchTeamDto.builder()
                        .id(entry.getTeam().getId().value())
                        .name(entry.getTeam().getName())
                        .build())
                .played(entry.getPlayed())
                .wins(entry.getWins())
                .draws(entry.getDraws())
                .defeats(entry.getDefeats())
                .goalsFor(entry.getGoalsFor())
                .goalsAgainst(entry.getGoalsAgainst())
                .goalDifference(entry.getGoalDifference())
                .points(entry.getPoints())
                .build();
    }
}
