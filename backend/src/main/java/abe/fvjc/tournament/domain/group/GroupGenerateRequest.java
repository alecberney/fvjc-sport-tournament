package abe.fvjc.tournament.group.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupGenerateRequest {
    Integer groupSize;
}
