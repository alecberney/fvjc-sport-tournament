package abe.fvjc.tournament.group.api;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupDistributionDto {
    int numberOfGroups;
    int groupsOfBaseSize;
    int groupsOfBaseSizePlusOne;
    int baseSize;
    int totalTeams;
}
