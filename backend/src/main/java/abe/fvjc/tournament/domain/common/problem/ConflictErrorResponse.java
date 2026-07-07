package abe.fvjc.tournament.domain.common.problem;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictErrorResponse {
    String error;
}
