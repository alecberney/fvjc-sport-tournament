package abe.fvjc.tournament.api.bracket;

import abe.fvjc.tournament.api.team.TeamRefDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class BracketMatchDto {
    UUID id;
    int field;
    TeamRefDto team1;
    TeamRefDto team2;
    BracketMatchResultDto result;
}
