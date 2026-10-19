import { BracketViewMapper } from '@app/domain/bracket/bracket.view.mapper';
import { BracketFakes } from '@app/domain/bracket/bracket.fakes';
import { BracketRound } from '@app/domain/bracket/bracket.model';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('BracketViewMapper', () => {

  const aBracket = (finalPlayed: boolean): BracketRound[] => [
    BracketFakes.aRound({
      id: 'round-semi', number: 1, name: 'Demi-finales',
      matches: [
        BracketFakes.aMatch({ id: 'semi-1', nextMatchId: 'final', loserNextMatchId: 'third' }),
        BracketFakes.aMatch({ id: 'semi-2', nextMatchId: 'final', loserNextMatchId: 'third' }),
      ],
    }),
    BracketFakes.aRound({
      id: 'round-final', number: 2, name: 'Finale',
      matches: [BracketFakes.aMatch({ id: 'final', result: finalPlayed ? { score1: 2, score2: 1 } : null })],
    }),
    BracketFakes.aRound({
      id: 'round-third', number: 3, name: 'Troisième place',
      matches: [BracketFakes.aMatch({ id: 'third', team1: null, team2: null })],
    }),
  ];

  describe('toBracketMatchView', () => {
    it('should map field, team names and score labels', () => {
      // setup
      const match = BracketFakes.aMatch({ field: 3, result: { score1: 3, score2: 1 } });

      // call
      const result = BracketViewMapper.toBracketMatchView(match, true, null);

      // assert
      expect(result).toEqual({
        id: match.id,
        fieldLabel: 'T3',
        team1Label: 'Les Aigles',
        team2Label: 'Les Loups',
        scoreLabel: '3 - 1',
        isSelectable: true,
        isSelected: false,
      });
    });

    it('should show placeholders when teams are unknown and no result', () => {
      // setup
      const match = BracketFakes.aMatch({ team1: null, team2: null, result: null });

      // call
      const result = BracketViewMapper.toBracketMatchView(match, true, null);

      // assert
      expect(result.team1Label).toBe('À déterminer');
      expect(result.team2Label).toBe('À déterminer');
      expect(result.scoreLabel).toBe('—');
      expect(result.isSelectable).toBe(false);
    });

    it('should not be selectable when results cannot be entered', () => {
      // setup
      const match = BracketFakes.aMatch();

      // call
      const result = BracketViewMapper.toBracketMatchView(match, false, null);

      // assert
      expect(result.isSelectable).toBe(false);
    });

    it('should be selected when its id is the selected match id', () => {
      // setup
      const match = BracketFakes.aMatch({ id: 'match-42' });

      // call
      const result = BracketViewMapper.toBracketMatchView(match, true, 'match-42');

      // assert
      expect(result.isSelected).toBe(true);
    });
  });

  describe('toBracketRoundView', () => {
    it('should keep name and start time and map every match', () => {
      // setup
      const round = BracketFakes.aRound({ matches: [BracketFakes.aMatch({ id: 'm1' }), BracketFakes.aMatch({ id: 'm2' })] });

      // call
      const result = BracketViewMapper.toBracketRoundView(round, true, null);

      // assert
      expect(result.name).toBe(round.name);
      expect(result.startTime).toBe(round.startTime);
      expect(result.matches.map(match => match.id)).toEqual(['m1', 'm2']);
    });
  });

  describe('toBracketSelectedMatchView', () => {
    it('should build the title and current scores', () => {
      // setup
      const match = BracketFakes.aMatch({ result: { score1: 2, score2: 0 } });

      // call
      const result = BracketViewMapper.toBracketSelectedMatchView(match);

      // assert
      expect(result).toEqual({
        id: match.id,
        title: 'Les Aigles vs Les Loups',
        team1Label: 'Les Aigles',
        team2Label: 'Les Loups',
        score1: 2,
        score2: 0,
      });
    });

    it('should have null scores when the match has no result', () => {
      // setup
      const match = BracketFakes.aMatch({ result: null });

      // call
      const result = BracketViewMapper.toBracketSelectedMatchView(match);

      // assert
      expect(result.score1).toBeNull();
      expect(result.score2).toBeNull();
    });
  });

  describe('toBracketPageView', () => {
    it('should allow ending when the tournament is in progress and the final is played', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();
      const rounds = aBracket(true);

      // call
      const result = BracketViewMapper.toBracketPageView(tournament, rounds, true, null);

      // assert
      expect(result.canEnd).toBe(true);
      expect(result.hasBracket).toBe(true);
      expect(result.isFinished).toBe(false);
      expect(result.rounds.length).toBe(3);
    });

    it('should expose the selected match while the tournament is in progress', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();
      const rounds = aBracket(false);

      // call
      const result = BracketViewMapper.toBracketPageView(tournament, rounds, true, 'semi-2');

      // assert
      expect(result.selectedMatch?.id).toBe('semi-2');
      expect(result.canEnd).toBe(false);
    });

    it('should drop the selection and lock matches when the tournament is finished', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();
      const rounds = aBracket(true);

      // call
      const result = BracketViewMapper.toBracketPageView(tournament, rounds, true, 'semi-1');

      // assert
      expect(result.isFinished).toBe(true);
      expect(result.selectedMatch).toBeNull();
      expect(result.rounds.flatMap(round => round.matches).some(match => match.isSelectable)).toBe(false);
    });

    it('should warn about missing group results when there is no bracket yet', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = BracketViewMapper.toBracketPageView(tournament, [], false, null);

      // assert
      expect(result.hasBracket).toBe(false);
      expect(result.showMissingResultsWarning).toBe(true);
    });
  });
});
