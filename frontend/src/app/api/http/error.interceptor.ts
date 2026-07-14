import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

import { ErrorMessageMapper } from '@app/api/http/error-message.mapper';

/**
 * Global HTTP error interceptor.
 * Shows a generic French message in a snackbar for every failed request and logs the
 * full error for debugging, then rethrows so NGXS actions and form logic still react.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      console.error('HTTP error', error);
      snackBar.open(ErrorMessageMapper.toMessage(error.status), 'Fermer', { duration: 5000 });
      return throwError(() => error);
    }),
  );
};
