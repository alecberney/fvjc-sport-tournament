import { ScheduleViewMapper } from '@app/domain/schedule/schedule.view.mapper';
import { ScheduleFakes } from '@app/domain/schedule/schedule.fakes';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('ScheduleViewMapper', () => {

  describe('toScheduleMatchView', () => {
    it('should map field, group and team names', () => {
      // setup
      const match = ScheduleFakes.aMatch({ field: 2 });

      // call
      const result = ScheduleViewMapper.toScheduleMatchView(match);

      // assert
      expect(result).toEqual({
        id: match.id,
        field: 2,
        groupName: 'Groupe A',
        team1Label: 'Les Aigles',
        team2Label: 'Les Lions',
      });
    });
  });

  describe('toScheduleRoundView', () => {
    it('should title the round by its number and keep its start time', () => {
      // setup
      const round = ScheduleFakes.aRound({ number: 3 });

      // call
      const result = ScheduleViewMapper.toScheduleRoundView(round);

      // assert
      expect(result.title).toBe('Round 3');
      expect(result.startTime).toBe(round.startTime);
      expect(result.matches.length).toBe(1);
    });
  });

  describe('toSchedulePageView', () => {
    it('should allow generating and starting when tournament is DRAFT with a schedule', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();
      const schedule = ScheduleFakes.aSchedule();

      // call
      const result = ScheduleViewMapper.toSchedulePageView(tournament, schedule);

      // assert
      expect(result.canGenerate).toBe(true);
      expect(result.canStart).toBe(true);
      expect(result.isEmpty).toBe(false);
      expect(result.rounds.length).toBe(1);
    });

    it('should be empty and not startable when no schedule is loaded', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = ScheduleViewMapper.toSchedulePageView(tournament, undefined);

      // assert
      expect(result.isEmpty).toBe(true);
      expect(result.canStart).toBe(false);
      expect(result.rounds).toEqual([]);
    });

    it('should forbid generating and starting when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();
      const schedule = ScheduleFakes.aSchedule();

      // call
      const result = ScheduleViewMapper.toSchedulePageView(tournament, schedule);

      // assert
      expect(result.canGenerate).toBe(false);
      expect(result.canStart).toBe(false);
    });
  });
});
