import { Tournament } from '@app/domain/tournament/tournament.model';

export interface BracketMatchView {
  id: string;
  fieldLabel: string;
  team1Label: string;
  team2Label: string;
  scoreLabel: string;
  isSelectable: boolean;
  isSelected: boolean;
}

export interface BracketRoundView {
  id: string;
  name: string;
  startTime: Date;
  matches: BracketMatchView[];
}

export interface BracketSelectedMatchView {
  id: string;
  title: string;
  team1Label: string;
  team2Label: string;
  score1: number | null;
  score2: number | null;
}

export interface BracketPageView {
  tournament: Tournament;
  rounds: BracketRoundView[];
  hasBracket: boolean;
  canEnd: boolean;
  isFinished: boolean;
  showMissingResultsWarning: boolean;
  selectedMatch: BracketSelectedMatchView | null;
}
