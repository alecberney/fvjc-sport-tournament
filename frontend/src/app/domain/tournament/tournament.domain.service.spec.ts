import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('TournamentDomainService', () => {

  describe('isDraft', () => {
    it('should return true when tournament status is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.isDraft(tournament);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.isDraft(tournament);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentDomainService.isDraft(tournament);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('isInProgress', () => {
    it('should return true when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.isInProgress(tournament);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.isInProgress(tournament);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentDomainService.isInProgress(tournament);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('isFinished', () => {
    it('should return true when tournament is FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentDomainService.isFinished(tournament);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.isFinished(tournament);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('canStart', () => {
    it('should return true when tournament is DRAFT and has a schedule', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.canStart(tournament, true);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is DRAFT without a schedule', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.canStart(tournament, false);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is already IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.canStart(tournament, true);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('canEnd', () => {
    it('should return true when tournament is IN_PROGRESS and the final is played', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.canEnd(tournament, true);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when the final is not played', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.canEnd(tournament, false);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is DRAFT even if the final is played', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.canEnd(tournament, true);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is already FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentDomainService.canEnd(tournament, true);

      // assert
      expect(result).toBe(false);
    });
  });

  describe('canEnterResults', () => {
    it('should return true when tournament is IN_PROGRESS', () => {
      // setup
      const tournament = TournamentFakes.anInProgressTournament();

      // call
      const result = TournamentDomainService.canEnterResults(tournament);

      // assert
      expect(result).toBe(true);
    });

    it('should return false when tournament is DRAFT', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentDomainService.canEnterResults(tournament);

      // assert
      expect(result).toBe(false);
    });

    it('should return false when tournament is FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentDomainService.canEnterResults(tournament);

      // assert
      expect(result).toBe(false);
    });
  });
});
