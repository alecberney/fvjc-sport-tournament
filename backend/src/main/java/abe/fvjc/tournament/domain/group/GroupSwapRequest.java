package abe.fvjc.tournament.domain.group;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class GroupSwapRequest {
    UUID teamId1;
    UUID teamId2;
}
