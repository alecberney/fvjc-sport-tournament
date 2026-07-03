export class GenerateSchedule {
  static readonly type = '[Schedule] Generate Schedule';
  constructor(
    public readonly tournamentId: string,
    public readonly startTime: string,
    public readonly matchDurationMinutes: number,
    public readonly breakDurationMinutes: number,
  ) {}
}

export class LoadSchedule {
  static readonly type = '[Schedule] Load Schedule';
  constructor(public readonly tournamentId: string) {}
}
