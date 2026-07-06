export interface GroupRankingEntry {
  rank: number;
  team: { id: string; name: string };
  played: number;
  wins: number;
  draws: number;
  defeats: number;
  goalsFor: number;
  goalsAgainst: number;
  goalDifference: number;
  points: number;
}

export interface GroupRanking {
  groupId: string;
  groupName: string;
  entries: GroupRankingEntry[];
}
