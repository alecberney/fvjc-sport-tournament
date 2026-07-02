import { TournamentDto, TournamentCreateRequestDto } from '@app/api/tournament/tournament.api.dto';
import { Tournament } from '@app/domain/tournament/tournament.model';

export class TournamentApiMapper {

  static toDomain(dto: TournamentDto): Tournament {
    return {
      ...dto,
      date: new Date(dto.date),
    };
  }

  static toCreateRequest(tournament: Partial<Tournament>): TournamentCreateRequestDto {
    return {
      name: tournament.name!,
      sport: tournament.sport!,
      numberOfFields: tournament.numberOfFields!,
      minPlayersPerTeam: tournament.minPlayersPerTeam!,
      maxPlayersPerTeam: tournament.maxPlayersPerTeam!,
      date: tournament.date!.toISOString().split('T')[0],
    };
  }
}
