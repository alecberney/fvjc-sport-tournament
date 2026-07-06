package abe.fvjc.tournament.schedule.api;

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
    MatchTeamDto team1;
    MatchTeamDto team2;
    MatchResultDto result;
}
