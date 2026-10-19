import { TournamentViewMapper } from '@app/domain/tournament/tournament.view.mapper';
import { TournamentFakes } from '@app/domain/tournament/tournament.fakes';

describe('TournamentViewMapper', () => {

  describe('toTournamentCardView', () => {
    it('should map labels for a draft tournament', () => {
      // setup
      const tournament = TournamentFakes.aDraftTournament();

      // call
      const result = TournamentViewMapper.toTournamentCardView(tournament);

      // assert
      expect(result).toEqual({
        id: tournament.id,
        name: tournament.name,
        sport: tournament.sport,
        date: tournament.date,
        fieldsLabel: '4 terrain(s)',
        playersPerTeamLabel: '2–4 joueurs/équipe',
        statusLabel: 'Brouillon',
        isStarted: false,
      });
    });

    it('should show a single player count when min and max are equal', () => {
      // setup
      const tournament = TournamentFakes.aTournament({ minPlayersPerTeam: 3, maxPlayersPerTeam: 3 });

      // call
      const result = TournamentViewMapper.toTournamentCardView(tournament);

      // assert
      expect(result.playersPerTeamLabel).toBe('3 joueurs/équipe');
    });

    it('should be started with its status label when tournament is FINISHED', () => {
      // setup
      const tournament = TournamentFakes.aFinishedTournament();

      // call
      const result = TournamentViewMapper.toTournamentCardView(tournament);

      // assert
      expect(result.statusLabel).toBe('Terminé');
      expect(result.isStarted).toBe(true);
    });
  });

  describe('toTournamentListPageView', () => {
    it('should be empty when there are no tournaments', () => {
      // setup — (no data needed)

      // call
      const result = TournamentViewMapper.toTournamentListPageView([]);

      // assert
      expect(result.isEmpty).toBe(true);
      expect(result.tournaments).toEqual([]);
    });

    it('should map every tournament to a card', () => {
      // setup
      const tournaments = [TournamentFakes.aDraftTournament(), TournamentFakes.anInProgressTournament()];

      // call
      const result = TournamentViewMapper.toTournamentListPageView(tournaments);

      // assert
      expect(result.isEmpty).toBe(false);
      expect(result.tournaments.map(card => card.statusLabel)).toEqual(['Brouillon', 'En cours']);
    });
  });
});
