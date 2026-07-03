export interface MatchTeam {
  id: string;
  name: string;
}

export interface Match {
  id: string;
  field: number;
  groupId: string;
  groupName: string;
  team1: MatchTeam;
  team2: MatchTeam;
}

export interface Round {
  id: string;
  number: number;
  startTime: Date;
  matches: Match[];
}

export interface Schedule {
  totalRounds: number;
  totalMatches: number;
  rounds: Round[];
}
