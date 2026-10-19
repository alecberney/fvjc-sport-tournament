import { ResultDomainService } from '@app/domain/result/result.domain.service';
import { ResultFakes } from '@app/domain/result/result.fakes';
import { GroupFakes } from '@app/domain/group/group.fakes';

describe('ResultDomainService', () => {

  describe('findRankingsToDisplay', () => {
    const groups = GroupFakes.aList(3);
    const rankings = {
      'group-id-1': ResultFakes.aRanking({ groupId: 'group-id-1', groupName: 'A' }),
      'group-id-3': ResultFakes.aRanking({ groupId: 'group-id-3', groupName: 'C' }),
    };

    it('should return every loaded ranking in group order when no group is selected', () => {
      // setup — (shared groups and rankings)

      // call
      const result = ResultDomainService.findRankingsToDisplay(groups, rankings, null);

      // assert
      expect(result.map(ranking => ranking.groupName)).toEqual(['A', 'C']);
    });

    it('should return only the selected group ranking', () => {
      // setup — (shared groups and rankings)

      // call
      const result = ResultDomainService.findRankingsToDisplay(groups, rankings, 'C');

      // assert
      expect(result.map(ranking => ranking.groupName)).toEqual(['C']);
    });

    it('should return empty array when the selected group has no ranking loaded', () => {
      // setup — (shared groups and rankings)

      // call
      const result = ResultDomainService.findRankingsToDisplay(groups, rankings, 'B');

      // assert
      expect(result).toEqual([]);
    });
  });
});
