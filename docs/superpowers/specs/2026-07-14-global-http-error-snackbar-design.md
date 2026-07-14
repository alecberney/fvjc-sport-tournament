# Global HTTP Error Snackbar — Design

**Date:** 2026-07-14
**Scope:** Frontend (Angular 21, standalone, NGXS, Angular Material)

## Goal

Add a global HTTP error interceptor that shows a generic French error message in a
Material snackbar whenever any HTTP request fails, and logs the full error to the
console for debugging.

## Behaviour

- Triggers on **every** failed HTTP response, including `400` validation errors.
- The snackbar shows a **generic per-status French message** — the backend error body
  is **not** parsed for display, but the full `HttpErrorResponse` **is logged** to the
  console (`console.error`).
- The interceptor **rethrows** the error after showing the snackbar, so NGXS actions and
  form logic still observe the failure. The interceptor only *adds* the toast; it never
  swallows the error.

### Status → message mapping

| Status | Message (French)                                            |
|--------|-------------------------------------------------------------|
| `0`    | `Impossible de joindre le serveur. Vérifiez votre connexion.` |
| `400`  | `Requête invalide. Vérifiez les informations saisies.`      |
| `404`  | `Ressource introuvable.`                                    |
| `409`  | `Conflit : cette opération n'est pas possible dans l'état actuel.` |
| `500`  | `Une erreur serveur est survenue. Réessayez plus tard.`     |
| other  | `Une erreur est survenue.` (default)                        |

## Components

### 1. `api/http/error-message.mapper.ts`

Pure static class, no Angular dependency — unit-testable per the frontend test rules.

```typescript
export class ErrorMessageMapper {
  static toMessage(status: number): string { /* switch on status */ }
}
```

### 2. `api/http/error.interceptor.ts`

Functional `HttpInterceptorFn`:

```typescript
export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('HTTP error', error);
      inject(MatSnackBar).open(
        ErrorMessageMapper.toMessage(error.status),
        'Fermer',
        { duration: 5000 },
      );
      return throwError(() => error);
    }),
  );
```

- Injects `MatSnackBar` directly (the interceptor is the only consumer — YAGNI, no
  wrapper service). If success toasts are needed later, extract a `NotificationService`
  then.

### 3. Wiring — `app.config.ts`

- `provideHttpClient(withInterceptors([errorInterceptor]))`
- No extra Material module needed: `MatSnackBar` works standalone with the animations
  provider already present (`provideAnimationsAsync()`).

## Testing

Per the frontend test rules, only the pure static method is unit-tested:

- `api/http/error-message.mapper.spec.ts`
  - one `describe('toMessage')` with an `it` per status (`0`, `400`, `404`, `409`, `500`)
    plus an unknown-status default case.

The interceptor and wiring are not unit-tested (side-effectful, Angular-dependent).

## Out of scope

- Parsing the backend error body for user-facing messages.
- Retry / offline handling.
- Success or informational snackbars.
