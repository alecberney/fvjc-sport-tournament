export enum TieBreaker {
  POINTS_SCORED = 'POINTS_SCORED',
  POINTS_DIFF = 'POINTS_DIFF',
  POINTS_TAKEN = 'POINTS_TAKEN',
}

export interface BracketTeam {
  id: string;
  name: string;
}

export interface BracketMatchResult {
  score1: number;
  score2: number;
}

export interface BracketMatch {
  id: string;
  field: number;
  team1: BracketTeam | null;
  team2: BracketTeam | null;
  result: BracketMatchResult | null;
}

export interface BracketRound {
  id: string;
  number: number;
  name: string;
  startTime: Date;
  matches: BracketMatch[];
}
