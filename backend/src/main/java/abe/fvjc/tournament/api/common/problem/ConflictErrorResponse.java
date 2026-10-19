package abe.fvjc.tournament.api.common.problem;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictErrorResponse {
    String error;
}
