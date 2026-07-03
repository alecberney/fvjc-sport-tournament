import { Schedule, Round, Match } from '@app/domain/schedule/schedule.model';
import { MatchDto, RoundDto, ScheduleDto } from '@app/api/schedule/schedule.api.dto';

export class ScheduleApiMapper {

  static toDomain(dto: ScheduleDto): Schedule {
    return {
      totalRounds: dto.totalRounds,
      totalMatches: dto.totalMatches,
      rounds: dto.rounds.map(ScheduleApiMapper.toRoundDomain),
    };
  }

  private static toRoundDomain(dto: RoundDto): Round {
    return {
      id: dto.id,
      number: dto.number,
      startTime: new Date(dto.startTime),
      matches: dto.matches.map(ScheduleApiMapper.toMatchDomain),
    };
  }

  static toMatchDomain(dto: MatchDto): Match {
    return {
      id: dto.id,
      field: dto.field,
      groupId: dto.groupId,
      groupName: dto.groupName,
      team1: dto.team1,
      team2: dto.team2,
      result: dto.result ? { score1: dto.result.score1, score2: dto.result.score2 } : null,
    };
  }
}
