import { GroupDto, GroupTeamDto } from '@app/api/group/group.api.dto';
import { Group } from '@app/domain/group/group.model';
import { Team } from '@app/domain/team/team.model';

export class GroupApiMapper {

  static toDomain(dto: GroupDto): Group {
    return {
      id: dto.id,
      name: dto.name,
      teams: dto.teams.map(GroupApiMapper.teamToDomain),
    };
  }

  private static teamToDomain(dto: GroupTeamDto): Team {
    return {
      id: dto.id,
      name: dto.name,
      organisationId: dto.organisationId,
      paid: false,
      responsibleFirstName: '',
      responsibleLastName: '',
    };
  }
}
