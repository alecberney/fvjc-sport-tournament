import { Team } from '@app/domain/team/team.model';
import { Tournament } from '@app/domain/tournament/tournament.model';

export interface TeamRowView {
  id: string;
  name: string;
  paid: boolean;
  paidIcon: string;
  paidColor: string;
  paidToggleTitle: string;
  team: Team;
}

export interface TeamOrganisationView {
  organisationId: string;
  responsibleName: string;
  teams: TeamRowView[];
}

export interface TeamPageView {
  tournament: Tournament;
  canEditTeams: boolean;
  isEmpty: boolean;
  organisations: TeamOrganisationView[];
}
