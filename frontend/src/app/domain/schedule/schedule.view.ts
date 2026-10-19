import { Tournament } from '@app/domain/tournament/tournament.model';

export interface ScheduleMatchView {
  id: string;
  field: number;
  groupName: string;
  team1Label: string;
  team2Label: string;
}

export interface ScheduleRoundView {
  id: string;
  title: string;
  startTime: Date;
  matches: ScheduleMatchView[];
}

export interface SchedulePageView {
  tournament: Tournament;
  canGenerate: boolean;
  canStart: boolean;
  isEmpty: boolean;
  rounds: ScheduleRoundView[];
}
