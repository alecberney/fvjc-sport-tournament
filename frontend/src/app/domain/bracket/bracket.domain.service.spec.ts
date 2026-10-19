import { BracketDomainService } from '@app/domain/bracket/bracket.domain.service';
import { BracketFakes } from '@app/domain/bracket/bracket.fakes';
import { BracketRound } from '@app/domain/bracket/bracket.model';

describe('BracketDomainService', () => {

  const aBracket = (finalPlayed: boolean, thirdPlacePlayed: boolean): BracketRound[] => [
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
      matches: [BracketFakes.aMatch({ id: 'third', result: thirdPlacePlayed ? { score1: 3, score2: 0 } : null })],
    }),
  ];

  describe('findFinal', () => {
    it('should return the final and not the third-place match', () => {
      // setup
      const rounds = aBracket(false, false);

      // call
      const result = BracketDomainService.findFinal(rounds);

      // assert
      expect(result?.id).toBe('final');
    });

    it('should return the single match when the bracket has only a final', () => {
      // setup
      const rounds = [BracketFakes.aRound({ matches: [BracketFakes.aMatch({ id: 'only-final' })] })];

      // call
      const result = BracketDomainService.findFinal(rounds);

      // assert
      expect(result?.id).toBe('only-final');
    });

    it('should return undefined when there is no bracket', () => {
      // setup — (no data needed)

      // call
      const result = BracketDomainService.findFinal([]);

      // assert
      expect(result).toBeUndefined();
    });
  });

  describe('isFinalPlayed', () => {
    it('should return true when the final has a result', () => {
      // setup
      const rounds = aBracket(true, false);

      // call
      const result = BracketDomainService.isFinalPlayed(rounds);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when only the third-place match has a result', () => {
      // setup
      const rounds = aBracket(false, true);

      // call
      const result = BracketDomainService.isFinalPlayed(rounds);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when there is no bracket', () => {
      // setup — (no data needed)

      // call
      const result = BracketDomainService.isFinalPlayed([]);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('isPlayable', () => {
    it('should return true when both teams are known', () => {
      // setup
      const match = BracketFakes.aMatch();

      // call
      const result = BracketDomainService.isPlayable(match);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when a team is not yet determined', () => {
      // setup
      const match = BracketFakes.aMatch({ team2: null });

      // call
      const result = BracketDomainService.isPlayable(match);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('findMatch', () => {
    it('should return the match with the given id across rounds', () => {
      // setup
      const rounds = aBracket(false, false);

      // call
      const result = BracketDomainService.findMatch(rounds, 'third');

      // assert
      expect(result?.id).toBe('third');
    });

    it('should return undefined when no id is given', () => {
      // setup
      const rounds = aBracket(false, false);

      // call
      const result = BracketDomainService.findMatch(rounds, null);

      // assert
      expect(result).toBeUndefined();
    });
  });
});
