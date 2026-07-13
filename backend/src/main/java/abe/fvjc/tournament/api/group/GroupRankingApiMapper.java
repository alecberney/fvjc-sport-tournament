package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.api.team.TeamRefDto;
import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class GroupRankingApiMapper {

    public static GroupRankingDto toGroupRankingDto(final GroupRanking ranking) {
        return GroupRankingDto.builder()
                .groupId(ranking.getGroupId().value())
                .groupName(ranking.getGroupName())
                .entries(toGroupRankingEntryDtos(ranking.getEntries()))
                .build();
    }

    static List<GroupRankingDto> toGroupRankingDtos(final List<GroupRanking> rankings) {
        return emptyIfNull(rankings).stream()
                .map(GroupRankingApiMapper::toGroupRankingDto)
                .toList();
    }

    private static GroupRankingEntryDto toGroupRankingEntryDto(final GroupRankingEntry entry) {
        return GroupRankingEntryDto.builder()
                .rank(entry.getRank())
                .team(TeamRefDto.builder()
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

    private static List<GroupRankingEntryDto> toGroupRankingEntryDtos(final List<GroupRankingEntry> entries) {
        return emptyIfNull(entries).stream()
                .map(GroupRankingApiMapper::toGroupRankingEntryDto)
                .toList();
    }
}
