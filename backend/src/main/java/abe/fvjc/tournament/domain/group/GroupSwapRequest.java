package abe.fvjc.tournament.group.domain;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class GroupSwapRequest {
    UUID teamId1;
    UUID teamId2;
}
