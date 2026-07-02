import { TeamDto, TeamRegisterRequestDto, TeamUpdateRequestDto } from '@app/api/team/team.api.dto';
import { Team, TeamRegistration, TeamUpdate } from '@app/domain/team/team.model';

export class TeamApiMapper {

  static toDomain(dto: TeamDto): Team {
    return { ...dto };
  }

  static toRegisterRequest(registration: TeamRegistration): TeamRegisterRequestDto {
    return {
      name: registration.name,
      responsibleFirstName: registration.responsibleFirstName,
      responsibleLastName: registration.responsibleLastName,
      count: registration.count,
      paid: registration.paid,
    };
  }

  static toUpdateRequest(update: TeamUpdate): TeamUpdateRequestDto {
    return {
      name: update.name,
      responsibleFirstName: update.responsibleFirstName,
      responsibleLastName: update.responsibleLastName,
      paid: update.paid,
    };
  }
}
