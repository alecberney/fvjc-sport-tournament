package abe.fvjc.tournament.api.schedule;

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
    UUID groupId;
    String groupName;
    TeamRefDto team1;
    TeamRefDto team2;
    MatchResultDto result;
}
