package abe.fvjc.tournament.api.group;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class GroupGenerateRequestDto {
    @NotNull
    Integer groupSize;
}
