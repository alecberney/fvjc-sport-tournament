import { GroupRanking } from '@app/domain/result/result.model';
import { Tournament } from '@app/domain/tournament/tournament.model';

export interface ResultMatchView {
  id: string;
  fieldLabel: string;
  groupName: string;
  teamsLabel: string;
  scoreLabel: string | null;
  hasResult: boolean;
  isSelected: boolean;
}

export interface ResultRoundView {
  id: string;
  title: string;
  startTime: Date;
  matches: ResultMatchView[];
}

export interface ResultSelectedMatchView {
  id: string;
  title: string;
  subtitle: string;
  team1Label: string;
  team2Label: string;
  score1: number | null;
  score2: number | null;
  ranking: RankingGroupView | null;
}

export interface ResultPageView {
  tournament: Tournament;
  isDraft: boolean;
  isFinished: boolean;
  rounds: ResultRoundView[];
  selectedMatch: ResultSelectedMatchView | null;
}

export interface RankingGroupView {
  groupId: string;
  title: string;
  ranking: GroupRanking;
}

export interface RankingGroupOptionView {
  value: string | null;
  label: string;
}

export interface RankingPageView {
  tournament: Tournament;
  groupOptions: RankingGroupOptionView[];
  rankings: RankingGroupView[];
}
