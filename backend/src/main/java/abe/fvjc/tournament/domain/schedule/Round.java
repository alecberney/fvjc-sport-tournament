package abe.fvjc.tournament.domain.schedule;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@With
@Builder
public class Round {
    RoundId id;
    TournamentId tournamentId;
    int number;
    LocalDateTime startTime;
}
