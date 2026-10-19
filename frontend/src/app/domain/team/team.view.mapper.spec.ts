import { TeamViewMapper } from '@app/domain/team/team.view.mapper';
import { TeamFakes } from '@app/domain/team/team.fakes';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('TeamViewMapper', () => {

  describe('toTeamRowView', () => {
    it('should show the paid icon and the unmark action when team has paid', () => {
      // setup
      const team = TeamFakes.aTeam({ paid: true });

      // call
      const result = TeamViewMapper.toTeamRowView(team);

      // assert
      expect(result.paidIcon).toBe('check_circle');
      expect(result.paidColor).toBe('primary');
      expect(result.paidToggleTitle).toBe('Marquer non payé');
      expect(result.team).toBe(team);
    });

    it('should show the unpaid icon and the mark action when team has not paid', () => {
      // setup
      const team = TeamFakes.aTeam({ paid: false });

      // call
      const result = TeamViewMapper.toTeamRowView(team);

      // assert
      expect(result.paidIcon).toBe('radio_button_unchecked');
      expect(result.paidColor).toBe('');
      expect(result.paidToggleTitle).toBe('Marquer payé');
    });
  });

  describe('toTeamPageView', () => {
    it('should group teams by organisation and allow edits when tournament is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();
      const teams = [
        TeamFakes.aTeam({ id: 't1', organisationId: 'org-1' }),
        TeamFakes.aTeam({ id: 't2', organisationId: 'org-2', responsibleFirstName: 'Marie', responsibleLastName: 'Rochat' }),
        TeamFakes.aTeam({ id: 't3', organisationId: 'org-1' }),
      ];

      // call
      const result = TeamViewMapper.toTeamPageView(tournament, teams);

      // assert
      expect(result.canEditTeams).toBe(true);
      expect(result.isEmpty).toBe(false);
      expect(result.organisations.map(org => org.responsibleName)).toEqual(['Jean Dupont', 'Marie Rochat']);
      expect(result.organisations[0].teams.map(team => team.id)).toEqual(['t1', 't3']);
    });

    it('should forbid edits and be empty when tournament is IN_PROGRESS without teams', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TeamViewMapper.toTeamPageView(tournament, []);

      // assert
      expect(result.canEditTeams).toBe(false);
      expect(result.isEmpty).toBe(true);
    });
  });
});
