package abe.fvjc.tournament.group.api;

import abe.fvjc.tournament.group.domain.GroupDistribution;
import abe.fvjc.tournament.group.domain.GroupGenerateRequest;
import abe.fvjc.tournament.group.domain.GroupSwapRequest;
import abe.fvjc.tournament.group.domain.GroupOverview;
import abe.fvjc.tournament.team.domain.Team;
import lombok.experimental.UtilityClass;

@UtilityClass
public class GroupApiMapper {
    static GroupDto toGroupDto(final GroupOverview overview) {
        return GroupDto.builder()
                .id(overview.getId().value())
                .name(overview.getName())
                .teams(overview.getTeams()
                        .stream()
                        .map(GroupApiMapper::toGroupTeamDto)
                        .toList())
                .build();
    }

    static GroupTeamDto toGroupTeamDto(final Team team) {
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
