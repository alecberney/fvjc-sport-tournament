import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, EMPTY } from 'rxjs';
import { TournamentDto, TournamentCreateRequestDto } from './tournament.api.dto';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  getAll$(): Observable<TournamentDto[]> {
    return EMPTY;
  }

  getById$(id: string): Observable<TournamentDto> {
    return EMPTY;
  }

  create$(request: TournamentCreateRequestDto): Observable<TournamentDto> {
    return EMPTY;
  }

  delete$(id: string): Observable<void> {
    return EMPTY;
  }
}
