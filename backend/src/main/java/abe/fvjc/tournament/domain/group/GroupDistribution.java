package abe.fvjc.tournament.domain.group;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupDistribution {
    int numberOfGroups;
    int groupsOfBaseSize;
    int groupsOfBaseSizePlusOne;
    int baseSize;
    int totalTeams;
}
