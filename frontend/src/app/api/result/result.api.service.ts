import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TournamentDto } from '@app/api/tournament/tournament.api.dto';
import { GroupRankingDto, MatchResultResponseDto, SubmitMatchResultRequestDto } from '@app/api/result/result.api.dto';

@Injectable({ providedIn: 'root' })
export class ResultApiService {

  private readonly http = inject(HttpClient);

  startTournament$(tournamentId: string): Observable<TournamentDto> {
    return this.http.post<TournamentDto>(`/api/tournaments/${tournamentId}/start`, {});
  }

  submitResult$(tournamentId: string, matchId: string, request: SubmitMatchResultRequestDto): Observable<MatchResultResponseDto> {
    return this.http.put<MatchResultResponseDto>(`/api/tournaments/${tournamentId}/matches/${matchId}/result`, request);
  }

  loadGroupRanking$(tournamentId: string, groupId: string): Observable<GroupRankingDto> {
    return this.http.get<GroupRankingDto>(`/api/tournaments/${tournamentId}/groups/${groupId}/ranking`);
  }
}
