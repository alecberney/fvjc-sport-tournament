package abe.fvjc.tournament.bracket.domain;

import abe.fvjc.tournament.tournament.domain.TournamentId;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
@With
public class BracketRound {
    BracketRoundId id;
    TournamentId tournamentId;
    int number;
    String name;
    LocalDateTime startTime;
    List<BracketMatch> matches;
}
