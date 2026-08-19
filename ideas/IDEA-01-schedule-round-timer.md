# IDEA-01 — Round-by-round schedule view with a live timer

**Status:** proposed
**Scope:** frontend (+ small backend addition, see Open questions)
**Impacted page:** `frontend/src/app/display/tournament/pages/tournament-schedule/`

---

## Problem

The schedule page (`tournament-schedule.page.html`) renders **every** round as a stacked
list of cards. On tournament day this has two consequences:

- On a phone, a 7-round schedule means a lot of scrolling to find the round being played.
- Nothing on the page says *what is happening right now*: no indication of the current
  round, of how much time is left before the next one, or of how far the tournament has
  progressed. Players and referees have to compute it themselves from `round.startTime`.

The schedule is a plan; on the day people need a **status board**.

---

## Proposed solution

Replace the "all rounds stacked" layout with a **single-round view** topped by a live
timer bar.

### 1. Single-round view with arrow navigation

- Only one round is displayed at a time (its matches table, unchanged).
- Left / right arrows on each side navigate to the previous / next round.
- Arrows are disabled at the first and last round.
- On load, the view opens on the **current round** (derived from the clock — see below),
  falling back to round 1 before the tournament starts and to the last round after it ends.
- A "Round en cours" affordance (button or label) returns to the current round after the
  user has navigated away.

### 2. Live timer bar on top

Driven by the **browser clock compared to the rounds' `startTime`** — read-only, derived,
no admin start/pause/reset control. Content:

| Element | Example | Notes |
|---|---|---|
| Current round | `Round 3 / 7` | The round happening now, not the one being browsed |
| Countdown | `⏱ 07:42 restant` | Time left in the current round, ticking every second |
| Next round | `→ Round 4 à 14:30` | Start time of the following round |
| Overall progress | `12 / 28 matchs joués` | From `schedule.totalMatches` and matches with a non-null `result` |

### 3. Live-round highlighting

- The current round is visually marked as live (badge / accent border).
- When the user browses to another round, the bar keeps showing the **current** round and
  the view makes clear it is not the one on screen (e.g. "Round 5 — à venir",
  "Round 2 — terminé").
- Past rounds read as finished, future rounds as upcoming.

### Sketch

```
┌───────────────────────────────────────────────────────────┐
│ ● EN COURS   Round 3 / 7      ⏱ 07:42 restant             │
│              12 / 28 matchs joués   → Round 4 à 14:30     │
└───────────────────────────────────────────────────────────┘

   ◀        Round 3 · 14:00        ▶     [ Round en cours ]
   ┌──────────────────────────────────────────────────────┐
   │ Terrain │ Groupe │ Équipe 1      │ Équipe 2          │
   │    1    │   A    │ Les Aigles    │ Les Lions         │
   │    2    │   B    │ …             │ …                 │
   └──────────────────────────────────────────────────────┘
```

---

## Data needed — and the blocker

The derived timer needs to know **when a round ends**. Today it cannot be computed:

- `Round` (backend domain) holds only `id`, `tournamentId`, `number`, `startTime`.
- `ScheduleGenerateRequest` carries `matchDurationMinutes` and `breakDurationMinutes`, but
  they are **used during generation and then discarded** — neither is persisted nor exposed
  in `ScheduleDto`.
- `startTime(round N+1) - startTime(round N)` gives *match + break*, so it overestimates
  the end of play, and it is undefined for the **last** round.

### Options

1. **Persist the two durations on the schedule (recommended).** Store
   `matchDurationMinutes` / `breakDurationMinutes` when the schedule is generated and expose
   them on `ScheduleDto`. The frontend then computes exactly:
   `roundEnd = startTime + matchDurationMinutes`, and the break window up to the next round.
   Cost: a Liquibase changelog, a domain field, mapper + DTO updates.
2. **Derive the span from consecutive `startTime`s.** No backend change, but the countdown
   includes the break and the last round has no end. Acceptable only as a first iteration.

---

## Frontend shape (follows the project rules)

- `ScheduleDomainService` gains **static** pure functions, unit-tested per
  `frontend-test-rules.md`:
  - `findCurrentRound(schedule, now)` → `Round | undefined`
  - `roundStatus(round, schedule, now)` → `PAST | LIVE | UPCOMING`
  - `remainingSeconds(round, now, matchDurationMinutes)` → `number`
  - `countPlayedMatches(schedule)` / `progressLabel(schedule)`
- The ticking clock is an `interval(1000)` observable in the page, combined with
  `schedule$`; every derivation stays in the domain service so it is testable without
  Angular.
- The timer bar is a pure presentational component
  (`components/schedule-timer/schedule-timer.component.ts`) receiving `@Input()`s only.
- Round navigation state (`displayedRoundNumber`) is local page state — no NGXS action.
- Compound `@if` conditions extracted into intent-named methods (`isLiveRound`,
  `canGoToNextRound`).

---

## Open questions

- Which of the two data options do we take — persist the durations, or ship the
  approximate version first?
- Does the timer keep running when the day drifts (a round starting late)? The derived
  approach silently desynchronises; a follow-up idea could add an admin "décaler l'horaire
  de N minutes" action.
- Should the single-round view also apply to the results page, or stay on the schedule?

---

## Out of scope

- Admin start / pause / reset controls and cross-device timer synchronisation.
- Push notifications or sound when a round ends.
- Knockout-stage bracket rounds (UC-07 / UC-08) — group-stage schedule only.
