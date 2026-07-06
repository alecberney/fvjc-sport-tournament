import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BracketGenerateRequestDto, BracketRoundDto } from '@app/api/bracket/bracket.api.dto';

@Injectable({ providedIn: 'root' })
export class BracketApiService {
  private readonly http = inject(HttpClient);

  loadBracket$(tournamentId: string): Observable<BracketRoundDto[]> {
    return this.http.get<BracketRoundDto[]>(`/api/tournaments/${tournamentId}/bracket`);
  }

  generateBracket$(tournamentId: string, request: BracketGenerateRequestDto): Observable<BracketRoundDto[]> {
    return this.http.post<BracketRoundDto[]>(`/api/tournaments/${tournamentId}/bracket/generate`, request);
  }
}
