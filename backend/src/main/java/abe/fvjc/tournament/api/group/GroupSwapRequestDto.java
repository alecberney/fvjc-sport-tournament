package abe.fvjc.tournament.api.group;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupSwapRequestDto {
    @NotNull
    UUID teamId1;
    @NotNull
    UUID teamId2;
}
