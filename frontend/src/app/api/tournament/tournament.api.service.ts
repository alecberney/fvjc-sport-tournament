import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TournamentDto, TournamentCreateRequestDto } from '@app/api/tournament/tournament.api.dto';

@Injectable({ providedIn: 'root' })
export class TournamentApiService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/tournaments';

  getAll$(): Observable<TournamentDto[]> {
    return this.http.get<TournamentDto[]>(this.baseUrl);
  }

  getById$(id: string): Observable<TournamentDto> {
    return this.http.get<TournamentDto>(`${this.baseUrl}/${id}`);
  }

  create$(request: TournamentCreateRequestDto): Observable<TournamentDto> {
    return this.http.post<TournamentDto>(this.baseUrl, request);
  }

  delete$(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
