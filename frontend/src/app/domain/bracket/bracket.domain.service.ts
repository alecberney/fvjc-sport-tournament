import { Injectable } from '@angular/core';
import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';

@Injectable({ providedIn: 'root' })
export class BracketDomainService {

  // The final is the only match that sends no winner further (no nextMatchId) and is not
  // the third-place match, which is the loser target of the semi-finals.
  static findFinal(rounds: BracketRound[]): BracketMatch | undefined {
    const matches = rounds.flatMap(round => round.matches);
    const loserTargetIds = new Set(matches.map(match => match.loserNextMatchId).filter(id => id !== null));
    return matches.find(match => match.nextMatchId === null && !loserTargetIds.has(match.id));
  }

  static isFinalPlayed(rounds: BracketRound[]): boolean {
    return !!BracketDomainService.findFinal(rounds)?.result;
  }
}
