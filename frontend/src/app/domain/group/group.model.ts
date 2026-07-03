import { Team } from '@app/domain/team/team.model';

export interface Group {
  id: string;
  name: string;
  teams: Team[];
}
