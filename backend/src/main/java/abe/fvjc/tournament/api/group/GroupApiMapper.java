package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.domain.group.GroupDistribution;
import abe.fvjc.tournament.domain.group.GroupGenerateRequest;
import abe.fvjc.tournament.domain.group.GroupSwapRequest;
import abe.fvjc.tournament.domain.group.GroupOverview;
import abe.fvjc.tournament.domain.group.GroupRef;
import abe.fvjc.tournament.domain.team.Team;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class GroupApiMapper {

    public static GroupRefDto toGroupRefDto(final GroupRef group) {
        return GroupRefDto.builder()
                .id(group.getId().value())
                .name(group.getName())
                .build();
    }

    static List<GroupDto> toGroupDtos(final List<GroupOverview> groups) {
        return emptyIfNull(groups).stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    private static GroupDto toGroupDto(final GroupOverview group) {
        return GroupDto.builder()
                .id(group.getId().value())
                .name(group.getName())
                .teams(toGroupTeamDtos(group.getTeams()))
                .build();
    }

    private static List<GroupTeamDto> toGroupTeamDtos(final List<Team> teams) {
        return emptyIfNull(teams).stream()
                .map(GroupApiMapper::toGroupTeamDto)
                .toList();
    }

    private static GroupTeamDto toGroupTeamDto(final Team team) {
        return GroupTeamDto.builder()
                .id(team.getId().value())
                .name(team.getName())
                .organisationId(team.getOrganisationId().value())
                .build();
    }

    static GroupDistributionDto toGroupDistributionDto(final GroupDistribution distribution) {
        return GroupDistributionDto.builder()
                .numberOfGroups(distribution.getNumberOfGroups())
                .groupsOfBaseSize(distribution.getGroupsOfBaseSize())
                .groupsOfBaseSizePlusOne(distribution.getGroupsOfBaseSizePlusOne())
                .baseSize(distribution.getBaseSize())
                .totalTeams(distribution.getTotalTeams())
                .build();
    }

    static GroupGenerateRequest toGroupGenerateRequest(final GroupGenerateRequestDto requestDto) {
        return GroupGenerateRequest.builder()
                .groupSize(requestDto.getGroupSize())
                .build();
    }

    static GroupSwapRequest toGroupSwapRequest(final GroupSwapRequestDto requestDto) {
        return GroupSwapRequest.builder()
                .teamId1(requestDto.getTeamId1())
                .teamId2(requestDto.getTeamId2())
                .build();
    }
}
