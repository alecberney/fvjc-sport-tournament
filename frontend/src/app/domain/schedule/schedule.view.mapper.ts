import { Match, Round, Schedule } from '@app/domain/schedule/schedule.model';
import { ScheduleMatchView, SchedulePageView, ScheduleRoundView } from '@app/domain/schedule/schedule.view';
import { ScheduleDomainService } from '@app/domain/schedule/schedule.domain.service';
import { Tournament } from '@app/domain/tournament/tournament.model';
import { TournamentDomainService } from '@app/domain/tournament/tournament.domain.service';

export class ScheduleViewMapper {

  static toSchedulePageView(tournament: Tournament, schedule: Schedule | undefined): SchedulePageView {
    const hasRounds = ScheduleDomainService.hasRounds(schedule);
    return {
      tournament,
      canGenerate: TournamentDomainService.isDraft(tournament),
      canStart: TournamentDomainService.canStart(tournament, hasRounds),
      isEmpty: !hasRounds,
      rounds: (schedule?.rounds ?? []).map(ScheduleViewMapper.toScheduleRoundView),
    };
  }

  static toScheduleRoundView(round: Round): ScheduleRoundView {
    return {
      id: round.id,
      title: `Round ${round.number}`,
      startTime: round.startTime,
      matches: round.matches.map(ScheduleViewMapper.toScheduleMatchView),
    };
  }

  static toScheduleMatchView(match: Match): ScheduleMatchView {
    return {
      id: match.id,
      field: match.field,
      groupName: match.groupName,
      team1Label: match.team1.name,
      team2Label: match.team2.name,
    };
  }
}
