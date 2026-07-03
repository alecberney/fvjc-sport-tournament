import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ScheduleDto, ScheduleGenerateRequestDto } from '@app/api/schedule/schedule.api.dto';

@Injectable({ providedIn: 'root' })
export class ScheduleApiService {

  private readonly http = inject(HttpClient);

  generate$(tournamentId: string, request: ScheduleGenerateRequestDto): Observable<ScheduleDto> {
    return this.http.post<ScheduleDto>(`/api/tournaments/${tournamentId}/schedule/generate`, request);
  }

  getSchedule$(tournamentId: string): Observable<ScheduleDto> {
    return this.http.get<ScheduleDto>(`/api/tournaments/${tournamentId}/schedule`);
  }
}
