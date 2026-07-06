import { BracketGenerateRequestDto } from '@app/api/bracket/bracket.api.dto';

export class LoadBracket {
  static readonly type = '[Bracket] Load Bracket';
  constructor(public readonly tournamentId: string) {}
}

export class GenerateBracket {
  static readonly type = '[Bracket] Generate Bracket';
  constructor(
    public readonly tournamentId: string,
    public readonly request: BracketGenerateRequestDto,
  ) {}
}

export class EnterBracketMatchResult {
  static readonly type = '[Bracket] Enter Match Result';
  constructor(
    public readonly tournamentId: string,
    public readonly matchId: string,
    public readonly score1: number,
    public readonly score2: number,
  ) {}
}
