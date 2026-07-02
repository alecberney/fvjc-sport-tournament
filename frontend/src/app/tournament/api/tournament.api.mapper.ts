import { TournamentDto, TournamentCreateRequestDto } from './tournament.api.dto';
import { Tournament } from '../domain/tournament.model';

export class TournamentApiMapper {

  static toDomain(dto: TournamentDto): Tournament {
    throw new Error('Not implemented');
  }

  static toCreateRequest(tournament: Partial<Tournament>): TournamentCreateRequestDto {
    throw new Error('Not implemented');
  }
}
