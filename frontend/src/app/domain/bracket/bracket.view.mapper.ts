import { BracketMatch, BracketRound } from '@app/domain/bracket/bracket.model';
import {
  BracketMatchView,
  BracketPageView,
  BracketRoundView,
  BracketSelectedMatchView,
} from '@app/domain/bracket/bracket.view';
import { BracketDomainService } from '@app/domain/bracket/bracket.domain.service';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class BracketViewMapper {

  static readonly TO_BE_DETERMINED = 'À déterminer';
  static readonly NO_SCORE = '—';

  static toBracketPageView(
    tournament: Tournament,
    rounds: BracketRound[],
    hasAllResults: boolean,
    selectedMatchId: string | null,
  ): BracketPageView {
    const canEnterResults = TournamentDomainService.canEnterResults(tournament);
    // A selection is only meaningful while results can be entered (cleared once the tournament ends).
    const selectedMatch = canEnterResults
      ? BracketDomainService.findMatch(rounds, selectedMatchId)
      : undefined;
    return {
      tournament,
      rounds: rounds.map((round) => BracketViewMapper.toBracketRoundView(round, canEnterResults, selectedMatchId)),
      hasBracket: rounds.length > 0,
      canEnd: TournamentDomainService.canEnd(tournament, BracketDomainService.isFinalPlayed(rounds)),
      isFinished: TournamentDomainService.isFinished(tournament),
      showMissingResultsWarning: !hasAllResults,
      selectedMatch: selectedMatch ? BracketViewMapper.toBracketSelectedMatchView(selectedMatch) : null,
    };
  }

  static toBracketRoundView(
    round: BracketRound,
    canEnterResults: boolean,
    selectedMatchId: string | null,
  ): BracketRoundView {
    return {
      id: round.id,
      name: round.name,
      startTime: round.startTime,
      matches: round.matches.map((match) => BracketViewMapper.toBracketMatchView(match, canEnterResults, selectedMatchId)),
    };
  }

  static toBracketMatchView(
    match: BracketMatch,
    canEnterResults: boolean,
    selectedMatchId: string | null,
  ): BracketMatchView {
    return {
      id: match.id,
      fieldLabel: `T${match.field}`,
      team1Label: match.team1?.name ?? BracketViewMapper.TO_BE_DETERMINED,
      team2Label: match.team2?.name ?? BracketViewMapper.TO_BE_DETERMINED,
      scoreLabel: match.result ? `${match.result.score1} - ${match.result.score2}` : BracketViewMapper.NO_SCORE,
      isSelectable: canEnterResults && BracketDomainService.isPlayable(match),
      isSelected: match.id === selectedMatchId,
    };
  }

  static toBracketSelectedMatchView(match: BracketMatch): BracketSelectedMatchView {
    const team1Label = match.team1?.name ?? BracketViewMapper.TO_BE_DETERMINED;
    const team2Label = match.team2?.name ?? BracketViewMapper.TO_BE_DETERMINED;
    return {
      id: match.id,
      title: `${team1Label} vs ${team2Label}`,
      team1Label,
      team2Label,
      score1: match.result?.score1 ?? null,
      score2: match.result?.score2 ?? null,
    };
  }
}
