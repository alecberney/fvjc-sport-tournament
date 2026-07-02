import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TeamDto, TeamPaidRequestDto, TeamRegisterRequestDto, TeamUpdateRequestDto } from '@app/api/team/team.api.dto';

@Injectable({ providedIn: 'root' })
export class TeamApiService {

  private readonly http = inject(HttpClient);

  private baseUrl(tournamentId: string): string {
    return `/api/tournaments/${tournamentId}/teams`;
  }

  getAll$(tournamentId: string): Observable<TeamDto[]> {
    return this.http.get<TeamDto[]>(this.baseUrl(tournamentId));
  }

  register$(tournamentId: string, request: TeamRegisterRequestDto): Observable<TeamDto[]> {
    return this.http.post<TeamDto[]>(this.baseUrl(tournamentId), request);
  }

  update$(tournamentId: string, teamId: string, request: TeamUpdateRequestDto): Observable<TeamDto> {
    return this.http.put<TeamDto>(`${this.baseUrl(tournamentId)}/${teamId}`, request);
  }

  delete$(tournamentId: string, teamId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl(tournamentId)}/${teamId}`);
  }

  markPaid$(tournamentId: string, teamId: string, paid: boolean): Observable<TeamDto> {
    const body: TeamPaidRequestDto = { paid };
    return this.http.patch<TeamDto>(`${this.baseUrl(tournamentId)}/${teamId}/paid`, body);
  }
}
