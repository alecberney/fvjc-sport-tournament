import { BracketMatch, BracketMatchResult, BracketRound, BracketTeam } from '@app/domain/bracket/bracket.model';
import { BracketMatchDto, BracketMatchResultRequestDto, BracketRoundDto } from '@app/api/bracket/bracket.api.dto';

export class BracketApiMapper {
  static toRoundDomain(dto: BracketRoundDto): BracketRound {
    return {
      id: dto.id,
      number: dto.number,
      name: dto.name,
      startTime: new Date(dto.startTime),
      matches: dto.matches.map(BracketApiMapper.toMatchDomain),
    };
  }

  private static toMatchDomain(dto: BracketMatchDto): BracketMatch {
    return {
      id: dto.id,
      field: dto.field,
      team1: dto.team1 ? ({ id: dto.team1.id, name: dto.team1.name } as BracketTeam) : null,
      team2: dto.team2 ? ({ id: dto.team2.id, name: dto.team2.name } as BracketTeam) : null,
      result: dto.result ? ({ score1: dto.result.score1, score2: dto.result.score2 } as BracketMatchResult) : null,
    };
  }

  static toSubmitResultRequest(score1: number, score2: number): BracketMatchResultRequestDto {
    return { score1, score2 };
  }
}
