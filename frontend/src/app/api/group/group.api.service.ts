import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GroupDto, GroupGenerateRequestDto, GroupSwapRequestDto } from '@app/api/group/group.api.dto';

@Injectable({ providedIn: 'root' })
export class GroupApiService {

  private readonly http = inject(HttpClient);

  generate$(tournamentId: string, request: GroupGenerateRequestDto): Observable<GroupDto[]> {
    return this.http.post<GroupDto[]>(`/api/tournaments/${tournamentId}/groups/generate`, request);
  }

  getAll$(tournamentId: string): Observable<GroupDto[]> {
    return this.http.get<GroupDto[]>(`/api/tournaments/${tournamentId}/groups`);
  }

  swap$(tournamentId: string, request: GroupSwapRequestDto): Observable<GroupDto[]> {
    return this.http.post<GroupDto[]>(`/api/tournaments/${tournamentId}/groups/swap`, request);
  }
}
