package abe.fvjc.tournament.domain.bracket;

import abe.fvjc.tournament.domain.tournament.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

@Value
@With
@Builder
public class BracketRound {
    BracketRoundId id;
    TournamentId tournamentId;
    int number;
    String name;
    LocalDateTime startTime;
    List<BracketMatch> matches;
}
