export interface MatchTeamDto {
  id: string;
  name: string;
}

export interface MatchDto {
  id: string;
  field: number;
  groupId: string;
  groupName: string;
  team1: MatchTeamDto;
  team2: MatchTeamDto;
}

export interface RoundDto {
  id: string;
  number: number;
  startTime: string;
  matches: MatchDto[];
}

export interface ScheduleDto {
  totalRounds: number;
  totalMatches: number;
  rounds: RoundDto[];
}

export interface ScheduleGenerateRequestDto {
  startTime: string;
  matchDurationMinutes: number;
  breakDurationMinutes: number;
}
