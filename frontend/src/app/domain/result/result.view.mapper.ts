import { Group } from '@app/domain/group/group.model';
import { GroupRanking } from '@app/domain/result/result.model';
import {
  RankingGroupOptionView,
  RankingGroupView,
  RankingPageView,
  ResultMatchView,
  ResultPageView,
  ResultRoundView,
  ResultSelectedMatchView,
} from '@app/domain/result/result.view';
import { ResultDomainService } from '@app/domain/result/result.domain.service';
import { Match, Round, Schedule } from '@app/domain/schedule/schedule.model';
import { ScheduleDomainService } from '@app/domain/schedule/schedule.domain.service';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class ResultViewMapper {

  static readonly ALL_GROUPS_LABEL = 'Tous les groupes';

  static toResultPageView(
    tournament: Tournament,
    schedule: Schedule | undefined,
    rankings: { [groupId: string]: GroupRanking },
    selectedMatchId: string | null,
  ): ResultPageView {
    const selectedMatch = ScheduleDomainService.findMatch(schedule, selectedMatchId);
    return {
      tournament,
      isDraft: TournamentDomainService.isDraft(tournament),
      isFinished: TournamentDomainService.isFinished(tournament),
      rounds: (schedule?.rounds ?? []).map((round) => ResultViewMapper.toResultRoundView(round, selectedMatchId)),
      selectedMatch: selectedMatch ? ResultViewMapper.toResultSelectedMatchView(selectedMatch, rankings) : null,
    };
  }

  static toResultRoundView(round: Round, selectedMatchId: string | null): ResultRoundView {
    return {
      id: round.id,
      title: `Round ${round.number}`,
      startTime: round.startTime,
      matches: round.matches.map((match) => ResultViewMapper.toResultMatchView(match, selectedMatchId)),
    };
  }

  static toResultMatchView(match: Match, selectedMatchId: string | null): ResultMatchView {
    return {
      id: match.id,
      fieldLabel: `T${match.field}`,
      groupName: match.groupName,
      teamsLabel: `${match.team1.name} vs ${match.team2.name}`,
      scoreLabel: match.result ? `${match.result.score1} - ${match.result.score2}` : null,
      hasResult: match.result !== null,
      isSelected: match.id === selectedMatchId,
    };
  }

  static toResultSelectedMatchView(
    match: Match,
    rankings: { [groupId: string]: GroupRanking },
  ): ResultSelectedMatchView {
    const ranking = rankings[match.groupId];
    return {
      id: match.id,
      title: `${match.team1.name} vs ${match.team2.name}`,
      subtitle: `${match.groupName} — Terrain ${match.field}`,
      team1Label: match.team1.name,
      team2Label: match.team2.name,
      score1: match.result?.score1 ?? null,
      score2: match.result?.score2 ?? null,
      ranking: ranking ? ResultViewMapper.toRankingGroupView(ranking) : null,
    };
  }

  static toRankingPageView(
    tournament: Tournament,
    groups: Group[],
    rankings: { [groupId: string]: GroupRanking },
    selectedGroupName: string | null,
  ): RankingPageView {
    return {
      tournament,
      selectedGroupName,
      groupOptions: ResultViewMapper.toRankingGroupOptionViews(groups),
      rankings: ResultDomainService.findRankingsToDisplay(groups, rankings, selectedGroupName)
        .map(ResultViewMapper.toRankingGroupView),
    };
  }

  static toRankingGroupView(ranking: GroupRanking): RankingGroupView {
    return {
      groupId: ranking.groupId,
      title: `Groupe ${ranking.groupName}`,
      ranking,
    };
  }

  // First option clears the filter (null = every group).
  static toRankingGroupOptionViews(groups: Group[]): RankingGroupOptionView[] {
    return [
      { value: null, label: ResultViewMapper.ALL_GROUPS_LABEL },
      ...groups.map((group) => ({ value: group.name, label: `Groupe ${group.name}` })),
    ];
  }
}
