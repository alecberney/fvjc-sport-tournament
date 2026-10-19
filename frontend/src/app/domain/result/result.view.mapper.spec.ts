import { ResultViewMapper } from '@app/domain/result/result.view.mapper';
import { ResultFakes } from '@app/domain/result/result.fakes';
import { GroupFakes } from '@app/domain/group/group.fakes';
import { ScheduleFakes } from '@app/domain/schedule/schedule.fakes';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('ResultViewMapper', () => {

  describe('toResultMatchView', () => {
    it('should map labels and score when the match has a result', () => {
      // setup
      const match = ScheduleFakes.aMatch({ id: 'm1', field: 2 });

      // call
      const result = ResultViewMapper.toResultMatchView(match, 'm1');

      // assert
      expect(result).toEqual({
        id: 'm1',
        fieldLabel: 'T2',
        groupName: 'Groupe A',
        teamsLabel: 'Les Aigles vs Les Lions',
        scoreLabel: '21 - 15',
        hasResult: true,
        isSelected: true,
      });
    });

    it('should have no score label when the match has no result', () => {
      // setup
      const match = ScheduleFakes.aMatch({ result: null });

      // call
      const result = ResultViewMapper.toResultMatchView(match, null);

      // assert
      expect(result.scoreLabel).toBeNull();
      expect(result.hasResult).toBe(false);
      expect(result.isSelected).toBe(false);
    });
  });

  describe('toResultSelectedMatchView', () => {
    it('should build title, subtitle, scores and the ranking of the match group', () => {
      // setup
      const match = ScheduleFakes.aMatch({ groupId: 'group-id-1', field: 3 });
      const ranking = ResultFakes.aRanking({ groupId: 'group-id-1', groupName: 'A' });

      // call
      const result = ResultViewMapper.toResultSelectedMatchView(match, { 'group-id-1': ranking });

      // assert
      expect(result.title).toBe('Les Aigles vs Les Lions');
      expect(result.subtitle).toBe('Groupe A — Terrain 3');
      expect(result.score1).toBe(21);
      expect(result.score2).toBe(15);
      expect(result.ranking?.title).toBe('Groupe A');
      expect(result.ranking?.ranking).toBe(ranking);
    });

    it('should have null scores and no ranking when neither is available', () => {
      // setup
      const match = ScheduleFakes.aMatch({ result: null });

      // call
      const result = ResultViewMapper.toResultSelectedMatchView(match, {});

      // assert
      expect(result.score1).toBeNull();
      expect(result.score2).toBeNull();
      expect(result.ranking).toBeNull();
    });
  });

  describe('toResultPageView', () => {
    it('should expose the selected match from the schedule', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();
      const schedule = ScheduleFakes.aSchedule();

      // call
      const result = ResultViewMapper.toResultPageView(tournament, schedule, {}, 'match-id-1');

      // assert
      expect(result.isDraft).toBe(false);
      expect(result.isFinished).toBe(false);
      expect(result.rounds[0].title).toBe('Round 1');
      expect(result.selectedMatch?.id).toBe('match-id-1');
    });

    it('should have no selected match and no rounds when no schedule is loaded', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = ResultViewMapper.toResultPageView(tournament, undefined, {}, 'match-id-1');

      // assert
      expect(result.isFinished).toBe(true);
      expect(result.rounds).toEqual([]);
      expect(result.selectedMatch).toBeNull();
    });
  });

  describe('toRankingGroupOptionViews', () => {
    it('should start with the all-groups option then one option per group', () => {
      // setup
      const groups = GroupFakes.aList(2);

      // call
      const result = ResultViewMapper.toRankingGroupOptionViews(groups);

      // assert
      expect(result).toEqual([
        { value: null, label: 'Tous les groupes' },
        { value: 'A', label: 'Groupe A' },
        { value: 'B', label: 'Groupe B' },
      ]);
    });
  });

  describe('toRankingPageView', () => {
    it('should show only the selected group ranking', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();
      const groups = GroupFakes.aList(2);
      const rankings = {
        'group-id-1': ResultFakes.aRanking({ groupId: 'group-id-1', groupName: 'A' }),
        'group-id-2': ResultFakes.aRanking({ groupId: 'group-id-2', groupName: 'B' }),
      };

      // call
      const result = ResultViewMapper.toRankingPageView(tournament, groups, rankings, 'B');

      // assert
      expect(result.selectedGroupName).toBe('B');
      expect(result.rankings.map(group => group.title)).toEqual(['Groupe B']);
      expect(result.groupOptions.length).toBe(3);
    });
  });
});
