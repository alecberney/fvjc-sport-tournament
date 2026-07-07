package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.domain.group.GroupRanking;
import abe.fvjc.tournament.domain.group.GroupRankingEntry;
import lombok.experimental.UtilityClass;

import java.util.List;

import static abe.fvjc.tournament.api.team.TeamApiMapper.toTeamRefDto;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class GroupRankingApiMapper {

    static List<GroupRankingDto> toGroupRankingDtos(final List<GroupRanking> rankings) {
        return emptyIfNull(rankings).stream()
                .map(GroupRankingApiMapper::toGroupRankingDto)
                .toList();
    }

    public static GroupRankingDto toGroupRankingDto(final GroupRanking ranking) {
        return GroupRankingDto.builder()
                .groupId(ranking.getGroupId().value())
                .groupName(ranking.getGroupName())
                .entries(toGroupRankingEntryDtos(ranking.getEntries()))
                .build();
    }

    private static List<GroupRankingEntryDto> toGroupRankingEntryDtos(final List<GroupRankingEntry> entries) {
        return emptyIfNull(entries).stream()
                .map(GroupRankingApiMapper::toGroupRankingEntryDto)
                .toList();
    }

    private static GroupRankingEntryDto toGroupRankingEntryDto(final GroupRankingEntry entry) {
        return GroupRankingEntryDto.builder()
                .rank(entry.getRank())
                .team(toTeamRefDto(entry.getTeam()))
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
