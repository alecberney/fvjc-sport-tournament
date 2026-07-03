import { TeamDomainService } from '@app/domain/team/team.domain.service';
import { TeamFakes } from '@app/domain/team/team.fakes';

describe('TeamDomainService', () => {

  describe('groupByOrganisation', () => {

    it('should return empty array when no teams', () => {
      // setup — (no data needed)

      // call
      const result = TeamDomainService.groupByOrganisation([]);

      // assert
      expect(result).toEqual([]);
    });

    it('should group teams from the same organisation into one group', () => {
      // setup
      const teams = TeamFakes.aList(2, { organisationId: 'org-1', responsibleFirstName: 'Jean', responsibleLastName: 'Dupont' });

      // call
      const result = TeamDomainService.groupByOrganisation(teams);

      // assert
      expect(result).toHaveSize(1);
      expect(result[0].organisationId).toBe('org-1');
      expect(result[0].responsibleName).toBe('Jean Dupont');
      expect(result[0].teams).toHaveSize(2);
    });

    it('should create separate groups for different organisations', () => {
      // setup
      const team1 = TeamFakes.aTeam({ organisationId: 'org-1', responsibleFirstName: 'Jean', responsibleLastName: 'Dupont' });
      const team2 = TeamFakes.aTeam({ id: 'team-id-2', organisationId: 'org-2', responsibleFirstName: 'Marie', responsibleLastName: 'Martin' });

      // call
      const result = TeamDomainService.groupByOrganisation([team1, team2]);

      // assert
      expect(result).toHaveSize(2);
      expect(result[0].organisationId).toBe('org-1');
      expect(result[1].organisationId).toBe('org-2');
    });
  });
});
