package abe.fvjc.tournament.schedule.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;

@Value
@Builder
@With
public class Round {
    RoundId id;
    TournamentId tournamentId;
    int number;
    LocalDateTime startTime;
}
