export interface BracketTeamDto {
  id: string;
  name: string;
}

export interface BracketMatchResultDto {
  score1: number;
  score2: number;
}

export interface BracketMatchDto {
  id: string;
  field: number;
  team1: BracketTeamDto | null;
  team2: BracketTeamDto | null;
  result: BracketMatchResultDto | null;
}

export interface BracketRoundDto {
  id: string;
  number: number;
  name: string;
  startTime: string;
  matches: BracketMatchDto[];
}

export interface BracketGenerateRequestDto {
  totalQualifiedTeams: number;
  tieBreaker: string;
  startTime: string;
  matchDurationMinutes: number;
  breakDurationMinutes: number;
}
