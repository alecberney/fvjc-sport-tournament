package abe.fvjc.tournament.api.group;

import abe.fvjc.tournament.domain.group.GroupDistribution;
import abe.fvjc.tournament.domain.group.GroupGenerateRequest;
import abe.fvjc.tournament.domain.group.GroupSwapRequest;
import abe.fvjc.tournament.domain.group.GroupOverview;
import abe.fvjc.tournament.domain.team.Team;
import lombok.experimental.UtilityClass;

import java.util.List;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

@UtilityClass
public class GroupApiMapper {
    static GroupDto toGroupDto(final GroupOverview overview) {
        return GroupDto.builder()
                .id(overview.getId().value())
                .name(overview.getName())
                .teams(toGroupTeamDtos(overview.getTeams()))
                .build();
    }

    static List<GroupDto> toGroupDtos(final List<GroupOverview> overviews) {
        return emptyIfNull(overviews).stream()
                .map(GroupApiMapper::toGroupDto)
                .toList();
    }

    static GroupTeamDto toGroupTeamDto(final Team team) {
        return GroupTeamDto.builder()
                .id(team.getId().value())
                .name(team.getName())
                .organisationId(team.getOrganisationId().value())
                .build();
    }

    static List<GroupTeamDto> toGroupTeamDtos(final List<Team> teams) {
        return emptyIfNull(teams).stream()
                .map(GroupApiMapper::toGroupTeamDto)
                .toList();
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

    static GroupGenerateRequest toGroupGenerateRequest(final GroupGenerateRequestDto dto) {
        return GroupGenerateRequest.builder()
                .groupSize(dto.getGroupSize())
                .build();
    }

    static GroupSwapRequest toGroupSwapRequest(final GroupSwapRequestDto dto) {
        return GroupSwapRequest.builder()
                .teamId1(dto.getTeamId1())
                .teamId2(dto.getTeamId2())
                .build();
    }
}
