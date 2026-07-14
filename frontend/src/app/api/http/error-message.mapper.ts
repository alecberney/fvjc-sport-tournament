/**
 * Maps an HTTP status code to a generic, user-facing French error message.
 * Pure function — no Angular dependency, so it stays unit-testable in isolation.
 */
export class ErrorMessageMapper {

  static toMessage(status: number): string {
    switch (status) {
      // status 0 = no response reached the browser (network down, CORS, timeout)
      case 0:
        return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
      case 400:
        return 'Requête invalide. Vérifiez les informations saisies.';
      case 404:
        return 'Ressource introuvable.';
      case 409:
        return "Conflit : cette opération n'est pas possible dans l'état actuel.";
      case 500:
        return 'Une erreur serveur est survenue. Réessayez plus tard.';
      default:
        return 'Une erreur est survenue.';
    }
  }
}
