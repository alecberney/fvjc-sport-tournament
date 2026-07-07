package abe.fvjc.tournament.domain.schedule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubmitMatchResultRequest {
    Integer score1;
    Integer score2;
}
