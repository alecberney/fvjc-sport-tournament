package abe.fvjc.tournament.domain.group;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupGenerateRequest {
    Integer groupSize;
}
