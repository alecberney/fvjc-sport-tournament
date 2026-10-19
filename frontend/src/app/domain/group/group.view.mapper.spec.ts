import { GroupViewMapper } from '@app/domain/group/group.view.mapper';
import { GroupFakes } from '@app/domain/group/group.fakes';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('GroupViewMapper', () => {

  describe('toGroupPageView', () => {
    it('should allow group edits when tournament is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();
      const groups = GroupFakes.aList(2);

      // call
      const result = GroupViewMapper.toGroupPageView(tournament, groups);

      // assert
      expect(result.canEditGroups).toBe(true);
      expect(result.isEmpty).toBe(false);
      expect(result.groups).toBe(groups);
    });

    it('should forbid group edits and be empty when tournament is IN_PROGRESS without groups', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = GroupViewMapper.toGroupPageView(tournament, []);

      // assert
      expect(result.canEditGroups).toBe(false);
      expect(result.isEmpty).toBe(true);
    });
  });
});
