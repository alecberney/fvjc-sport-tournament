import { MatchDto } from '@app/api/schedule/schedule.api.dto';

export interface SubmitMatchResultRequestDto {
  score1: number;
  score2: number;
}

export interface GroupRankingEntryDto {
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

export interface GroupRankingDto {
  groupId: string;
  groupName: string;
  entries: GroupRankingEntryDto[];
}

export interface MatchResultResponseDto {
  match: MatchDto;
  ranking: GroupRankingDto;
}

export interface GroupRankingSearchRequestDto {
  groups?: string;
}
