import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngxs/store';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideNativeDateAdapter } from '@angular/material/core';

import { errorInterceptor } from '@app/api/http/error.interceptor';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor])),
    provideStore([]),
    provideAnimationsAsync(),
    provideNativeDateAdapter(),
  ],
};
