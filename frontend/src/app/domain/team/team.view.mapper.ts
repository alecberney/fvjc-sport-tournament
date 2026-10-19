import { Team, TeamGroup } from '@app/domain/team/team.model';
import { TeamOrganisationView, TeamPageView, TeamRowView } from '@app/domain/team/team.view';
import { TeamDomainService } from '@app/domain/team/team.domain.service';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class TeamViewMapper {

  static toTeamPageView(tournament: Tournament, teams: Team[]): TeamPageView {
    return {
      tournament,
      canEditTeams: TournamentDomainService.isDraft(tournament),
      isEmpty: teams.length === 0,
      organisations: TeamDomainService.groupByOrganisation(teams).map(TeamViewMapper.toTeamOrganisationView),
    };
  }

  static toTeamOrganisationView(group: TeamGroup): TeamOrganisationView {
    return {
      organisationId: group.organisationId,
      responsibleName: group.responsibleName,
      teams: group.teams.map(TeamViewMapper.toTeamRowView),
    };
  }

  static toTeamRowView(team: Team): TeamRowView {
    return {
      id: team.id,
      name: team.name,
      paid: team.paid,
      paidIcon: team.paid ? 'check_circle' : 'radio_button_unchecked',
      paidColor: team.paid ? 'primary' : '',
      paidToggleTitle: team.paid ? 'Marquer non payé' : 'Marquer payé',
      team,
    };
  }
}
