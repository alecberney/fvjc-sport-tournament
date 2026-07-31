import { ScheduleDomainService } from '@app/domain/schedule/schedule.domain.service';
import { ScheduleFakes } from '@app/domain/schedule/schedule.fakes';

describe('ScheduleDomainService', () => {

  describe('hasAllResults', () => {

    it('should return false when schedule is undefined', () => {
      // setup — (no data needed)

      // call
      const result = ScheduleDomainService.hasAllResults(undefined);

      // assert
      expect(result).toBe(false);
    });

    it('should return true when every match has a result', () => {
      // setup
      const schedule = ScheduleFakes.aSchedule({
        rounds: [
          ScheduleFakes.aRound({
            matches: [
              ScheduleFakes.aMatch({ result: { score1: 21, score2: 15 } }),
              ScheduleFakes.aMatch({ id: 'match-id-2', result: { score1: 10, score2: 21 } }),
            ],
          }),
        ],
      });

      // call
      const result = ScheduleDomainService.hasAllResults(schedule);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when a match has no result', () => {
      // setup
      const schedule = ScheduleFakes.aSchedule({
        rounds: [
          ScheduleFakes.aRound({
            matches: [
              ScheduleFakes.aMatch({ result: { score1: 21, score2: 15 } }),
              ScheduleFakes.aMatch({ id: 'match-id-2', result: null }),
            ],
          }),
        ],
      });

      // call
      const result = ScheduleDomainService.hasAllResults(schedule);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when a match in a later round has no result', () => {
      // setup
      const schedule = ScheduleFakes.aSchedule({
        rounds: [
          ScheduleFakes.aRound({ matches: [ScheduleFakes.aMatch({ result: { score1: 21, score2: 15 } })] }),
          ScheduleFakes.aRound({ id: 'round-id-2', number: 2, matches: [ScheduleFakes.aMatch({ id: 'match-id-2', result: null })] }),
        ],
      });

      // call
      const result = ScheduleDomainService.hasAllResults(schedule);

      // assert
      expect(result).toBe(false);
    });

    it('should return true when schedule has no rounds', () => {
      // setup
      const schedule = ScheduleFakes.aSchedule({ rounds: [] });

      // call
      const result = ScheduleDomainService.hasAllResults(schedule);

      // assert
      expect(result).toBe(true);
    });
  });
});
