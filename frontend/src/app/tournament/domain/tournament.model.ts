export enum TournamentStatus {
  DRAFT = 'DRAFT',
  IN_PROGRESS = 'IN_PROGRESS',
}

export enum Sport {
  PETANQUE = 'PETANQUE',
  VOLLEY = 'VOLLEY',
  LUTTE = 'LUTTE',
  TIR_A_LA_CORDE = 'TIR_A_LA_CORDE',
  FOOTBALL = 'FOOTBALL',
}

export interface Tournament {
  id: string;
  name: string;
  sport: Sport;
  numberOfFields: number;
  minPlayersPerTeam: number;
  maxPlayersPerTeam: number;
  date: Date;
  status: TournamentStatus;
}
