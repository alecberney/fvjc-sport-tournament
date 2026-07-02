import { Sport, TournamentStatus } from '../domain/tournament.model';

export interface TournamentDto {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: string;
  status: TournamentStatus;
}

export interface TournamentCreateRequestDto {
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: string;
}
