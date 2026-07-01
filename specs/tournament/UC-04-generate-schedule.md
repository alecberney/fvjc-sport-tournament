# UC-04 — Generate Group Stage Schedule

## Summary

The admin triggers schedule generation for the group stage. The system generates all round-robin matches for each group, assigns groups to fields, and organises matches into sequential rounds. Each round is a time slot where every field plays one match in parallel. The first generation transitions the tournament from `DRAFT` to `IN_PROGRESS`.

---

## Actor

Admin

---

## Preconditions

- Tournament exists and is in `DRAFT` status.
- Groups have been generated (UC-03).
- At least one group exists with at least 2 teams.

---

## Domain Concepts

### Schedule

The full match plan for the group stage. A tournament has at most one schedule at a time.

### Round

A time slot during which each field plays exactly one match simultaneously.

| Field         | Type          | Description                                      |
|---------------|---------------|--------------------------------------------------|
| `id`          | UUID          | —                                                |
| `number`      | Int           | Round index starting at 1                        |
| `startTime`   | LocalDateTime | Computed from tournament date + parameters       |
| `matches`     | List\<Match\> | One match per active field                       |

### Match

| Field        | Type   | Description                              |
|--------------|--------|------------------------------------------|
| `id`         | UUID   | —                                        |
| `roundId`    | UUID   | —                                        |
| `field`      | Int    | Field number (1 to numberOfFields)       |
| `groupId`    | UUID   | Group this match belongs to              |
| `team1Id`    | UUID   | —                                        |
| `team2Id`    | UUID   | —                                        |
| `result`     | null   | Filled in later (UC-05)                  |

---

## Scheduling Algorithm

### Step 1 — Generate round-robin matches per group

For a group of N teams, generate all `N*(N-1)/2` matches using the standard round-robin algorithm. Matches are ordered so that no team plays twice in the same group sub-round.

- Group of 4 teams → 6 matches (3 sub-rounds × 2 matches)
- Group of 5 teams → 10 matches (5 sub-rounds × 2 matches, 1 bye per sub-round)

### Step 2 — Assign groups to fields

Distribute groups across fields in order:

```
Group A → Field 1
Group B → Field 2
...
Group E → Field 1  (wraps around)
Group F → Field 2
...
```

If there are fewer groups than fields, the extra fields are unused.

### Step 3 — Build a match queue per field

For each field, interleave the matches of all groups assigned to it in round-robin rotation:

**Example — Field 1 has Groups A and E (6 matches each):**

| Queue position | Match         |
|----------------|---------------|
| 1              | Group A, match 1 |
| 2              | Group E, match 1 |
| 3              | Group A, match 2 |
| 4              | Group E, match 2 |
| 5              | Group A, match 3 |
| 6              | Group E, match 3 |
| 7              | Group A, match 4 |
| 8              | Group E, match 4 |
| …              | …             |

When one group's queue is exhausted, the remaining group continues filling its field slots alone.

**Example — Field 1 has Groups A (6 matches) and E (10 matches):**

Positions 1–12 interleave A and E. From position 13 onward, only Group E matches remain (A is done). Field 1 total: 16 slots.

**Example — Field 1 has 3 groups (A, E, I):**

Rotation: A1, E1, I1, A2, E2, I2, A3, E3, I3, …

### Step 4 — Build global rounds

The total number of rounds = length of the longest field queue.

Global Round K:
- Field 1 plays its queue[K] match (or is idle if its queue is exhausted)
- Field 2 plays its queue[K] match (or idle)
- …

A team never appears in two matches in the same global round because each team is assigned to exactly one field, and each field plays exactly one match per round.

### Step 5 — Assign times

```
Round 1 start = tournamentDate + startTime
Round N start = Round 1 start + (N-1) × (matchDurationMinutes + breakDurationMinutes)
```

---

## Status Transition

Schedule generation does **not** change the tournament status. The tournament stays in `DRAFT`.

The tournament transitions to `IN_PROGRESS` only when the admin explicitly starts it via a dedicated action (UC-05 — Start Tournament).

### Regeneration

Regeneration (calling generate again while a schedule already exists) is:
- **Allowed** if no match result has been entered yet — all existing rounds and matches are deleted and replaced.
- **Denied** if at least one match result exists — the admin must not lose recorded results by accident.

---

## Use Cases

### UC-04.1 — Generate (or Regenerate) Schedule

#### Validation Rules

| Rule | Error message |
|------|---------------|
| Tournament not in `DRAFT` | "Le calendrier ne peut être généré que pour un tournoi en préparation" |
| No groups exist | "Aucun groupe n'a été généré pour ce tournoi" |
| Schedule exists and at least one result entered | "Impossible de régénérer le calendrier : des résultats ont déjà été saisis" |
| `startTime` is null | "L'heure de début est obligatoire" |
| `matchDurationMinutes` is null | "La durée d'un match est obligatoire" |
| `matchDurationMinutes` < 1 | "La durée d'un match doit être d'au moins 1 minute" |
| `breakDurationMinutes` is null | "La durée de pause est obligatoire" |
| `breakDurationMinutes` < 0 | "La durée de pause ne peut pas être négative" |

#### API Contract

```
POST /api/tournaments/{tournamentId}/schedule/generate
```

```json
{
  "startTime": "09:00",
  "matchDurationMinutes": 20,
  "breakDurationMinutes": 5
}
```

**Response — `201 Created`**

```json
{
  "totalRounds": 12,
  "totalMatches": 42,
  "rounds": [
    {
      "id": "uuid-r1",
      "number": 1,
      "startTime": "2025-08-15T09:00:00",
      "matches": [
        {
          "id": "uuid-m1",
          "field": 1,
          "groupId": "uuid-group-a",
          "groupName": "A",
          "team1": { "id": "uuid-t1", "name": "Les Aigles" },
          "team2": { "id": "uuid-t2", "name": "Les Faucons" },
          "result": null
        },
        {
          "id": "uuid-m2",
          "field": 2,
          "groupId": "uuid-group-b",
          "groupName": "B",
          "team1": { "id": "uuid-t5", "name": "Les Loups" },
          "team2": { "id": "uuid-t6", "name": "Les Aigles Noirs" },
          "result": null
        }
      ]
    },
    {
      "id": "uuid-r2",
      "number": 2,
      "startTime": "2025-08-15T09:25:00",
      "matches": [...]
    }
  ]
}
```

**Response — Validation Error `400 Bad Request`**

```json
{
  "errors": [
    { "field": "startTime", "message": "L'heure de début est obligatoire" }
  ]
}
```

---

### UC-04.2 — Get Schedule

Returns the current schedule for a tournament.

#### API Contract

```
GET /api/tournaments/{tournamentId}/schedule
```

**Response — `200 OK`** — same structure as UC-04.1 response.

**Response — `200 OK` (no schedule yet)**

```json
{
  "totalRounds": 0,
  "totalMatches": 0,
  "rounds": []
}
```

---

## Domain Impact

- `Round` and `Match` are new aggregates under a `schedule` feature package.
- `Schedule` is not a persisted entity — it is a read projection assembled from `Round` and `Match`.
- Generating the schedule:
  1. Deletes all existing `Round` and `Match` entries for the tournament (if any, and only if no results exist).
  2. Creates new `Round` and `Match` entries.
  3. Does **not** change tournament status — status transition is handled by UC-05.
- A field with no match in a given round is simply absent from that round's match list (no idle placeholder stored).
