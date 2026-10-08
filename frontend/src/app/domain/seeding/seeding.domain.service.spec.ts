import { SeedingDomainService } from '@app/domain/seeding/seeding.domain.service';
import { ScheduleFakes } from '@app/domain/schedule/schedule.fakes';
import { BracketFakes } from '@app/domain/bracket/bracket.fakes';

describe('SeedingDomainService', () => {

  describe('buildRandomRegistrations', () => {

    it('should return empty array when count is 0', () => {
      // setup — (no data needed)

      // call
      const result = SeedingDomainService.buildRandomRegistrations(0);

      // assert
      expect(result).toEqual([]);
    });

    it('should register exactly the requested number of teams', () => {
      // setup
      const count = 17;

      // call
      const result = SeedingDomainService.buildRandomRegistrations(count);

      // assert
      const total = result.reduce((sum, registration) => sum + registration.count, 0);
      expect(total).toBe(count);
      result.forEach(registration => {
        expect(registration.count).toBeGreaterThanOrEqual(1);
        expect(registration.count).toBeLessThanOrEqual(3);
        expect(registration.paid).toHaveSize(registration.count);
      });
    });

    it('should give each organisation a distinct base name when names are exhausted', () => {
      // setup
      const alwaysOneTeam = () => 0;

      // call
      const result = SeedingDomainService.buildRandomRegistrations(13, alwaysOneTeam);

      // assert
      const names = result.map(registration => registration.name);
      expect(names).toHaveSize(13);
      expect(new Set(names).size).toBe(13);
      expect(names[12]).toBe('Les Aigles 2');
    });
  });

  describe('buildRandomResults', () => {

    it('should return empty array when schedule is undefined', () => {
      // setup — (no data needed)

      // call
      const result = SeedingDomainService.buildRandomResults(undefined);

      // assert
      expect(result).toEqual([]);
    });

    it('should only build results for matches without a result', () => {
      // setup
      const played = ScheduleFakes.aMatch({ id: 'match-played' });
      const unplayed = ScheduleFakes.aMatch({ id: 'match-unplayed', result: null });
      const schedule = ScheduleFakes.aSchedule({ rounds: [ScheduleFakes.aRound({ matches: [played, unplayed] })] });

      // call
      const result = SeedingDomainService.buildRandomResults(schedule);

      // assert
      expect(result).toHaveSize(1);
      expect(result[0].matchId).toBe('match-unplayed');
    });

    it('should build scores between 0 and 5 when random returns its bounds', () => {
      // setup
      const values = [0, 0.999];
      const random = () => values.shift()!;
      const schedule = ScheduleFakes.aSchedule({ rounds: [ScheduleFakes.aRound({ matches: [ScheduleFakes.aMatch({ result: null })] })] });

      // call
      const result = SeedingDomainService.buildRandomResults(schedule, random);

      // assert
      expect(result[0].score1).toBe(0);
      expect(result[0].score2).toBe(5);
    });
  });

  describe('buildRandomBracketResults', () => {

    it('should only build results for matches with both teams and no result', () => {
      // setup
      const ready = BracketFakes.aMatch({ id: 'match-ready' });
      const played = BracketFakes.aMatch({ id: 'match-played', result: { score1: 2, score2: 1 } });
      const waiting = BracketFakes.aMatch({ id: 'match-waiting', team2: null });
      const rounds = [BracketFakes.aRound({ matches: [ready, played, waiting] })];

      // call
      const result = SeedingDomainService.buildRandomBracketResults(rounds);

      // assert
      expect(result).toHaveSize(1);
      expect(result[0].matchId).toBe('match-ready');
    });

    it('should never build a draw when both random scores are equal', () => {
      // setup
      const values = [0.5, 0.5, 0.9];
      const random = () => values.shift()!;
      const rounds = [BracketFakes.aRound()];

      // call
      const result = SeedingDomainService.buildRandomBracketResults(rounds, random);

      // assert
      expect(result[0].score1).toBe(3);
      expect(result[0].score2).toBe(4);
    });
  });
});
