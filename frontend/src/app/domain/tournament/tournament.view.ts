import { Sport } from '@app/domain/tournament/tournament.model';

export interface TournamentCardView {
  id: string;
  name: string;
  sport: Sport;
  date: Date;
  fieldsLabel: string;
  playersPerTeamLabel: string;
  statusLabel: string;
  isStarted: boolean;
}

export interface TournamentListPageView {
  tournaments: TournamentCardView[];
  isEmpty: boolean;
}
