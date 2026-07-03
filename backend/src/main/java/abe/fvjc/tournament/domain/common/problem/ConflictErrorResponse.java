package abe.fvjc.tournament.shared.exception;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConflictErrorResponse {
    String error;
}
