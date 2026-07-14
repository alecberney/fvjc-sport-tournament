import { ErrorMessageMapper } from '@app/api/http/error-message.mapper';

describe('ErrorMessageMapper', () => {

  describe('toMessage', () => {
    it('should return connection message when status is 0', () => {
      // call
      const result = ErrorMessageMapper.toMessage(0);

      // assert
      expect(result).toBe('Impossible de joindre le serveur. Vérifiez votre connexion.');
    });

    it('should return invalid request message when status is 400', () => {
      // call
      const result = ErrorMessageMapper.toMessage(400);

      // assert
      expect(result).toBe('Requête invalide. Vérifiez les informations saisies.');
    });

    it('should return not found message when status is 404', () => {
      // call
      const result = ErrorMessageMapper.toMessage(404);

      // assert
      expect(result).toBe('Ressource introuvable.');
    });

    it('should return conflict message when status is 409', () => {
      // call
      const result = ErrorMessageMapper.toMessage(409);

      // assert
      expect(result).toBe("Conflit : cette opération n'est pas possible dans l'état actuel.");
    });

    it('should return server error message when status is 500', () => {
      // call
      const result = ErrorMessageMapper.toMessage(500);

      // assert
      expect(result).toBe('Une erreur serveur est survenue. Réessayez plus tard.');
    });

    it('should return default message when status is unknown', () => {
      // call
      const result = ErrorMessageMapper.toMessage(418);

      // assert
      expect(result).toBe('Une erreur est survenue.');
    });
  });
});
