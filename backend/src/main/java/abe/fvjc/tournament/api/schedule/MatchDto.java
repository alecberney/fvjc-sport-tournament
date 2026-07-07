package abe.fvjc.tournament.api.schedule;

import abe.fvjc.tournament.api.group.GroupRefDto;
import abe.fvjc.tournament.api.team.TeamRefDto;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class MatchDto {
    UUID id;
    int field;
    GroupRefDto group;
    TeamRefDto team1;
    TeamRefDto team2;
    MatchResultDto result;
}
