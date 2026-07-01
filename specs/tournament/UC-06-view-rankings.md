# UC-06 — View Group Rankings

## Summary

A dedicated rankings page shows all group standings, computed from entered match results. The page is accessible in both `DRAFT` and `IN_PROGRESS` status. The admin can filter which groups to display.

---

## Actor

Admin

---

## Preconditions

- Tournament exists (any status).
- At least one group exists.

---

## Main Flow

1. Admin navigates to the rankings page.
2. All groups are displayed by default, each with its ranked team list.
3. Admin can select/deselect specific groups to show using a filter.
4. Rankings update live as results are entered (in `IN_PROGRESS`).

---

## Ranking Computation

Same rules as UC-05:

| Outcome | Points |
|---------|--------|
| Win     | 2      |
| Draw    | 1      |
| Defeat  | 0      |

**Ordering within a group:** points DESC → goalDifference DESC → goalsFor DESC. Fully tied teams share the same rank.

Teams with no played matches appear at the bottom with all values at 0.

---

## API Contract

### Get All Group Rankings

```
GET /api/tournaments/{tournamentId}/groups/rankings
```

Optional filter by group names:

```
GET /api/tournaments/{tournamentId}/groups/rankings?groups=A,B,C
```

**Response — `200 OK`**

```json
[
  {
    "groupId": "uuid-group-a",
    "groupName": "A",
    "entries": [
      {
        "rank": 1,
        "team": { "id": "uuid-t1", "name": "Les Aigles" },
        "played": 3,
        "wins": 2,
        "draws": 1,
        "defeats": 0,
        "goalsFor": 7,
        "goalsAgainst": 3,
        "goalDifference": 4,
        "points": 5
      },
      {
        "rank": 2,
        "team": { "id": "uuid-t2", "name": "Les Faucons" },
        "played": 2,
        "wins": 1,
        "draws": 0,
        "defeats": 1,
        "goalsFor": 4,
        "goalsAgainst": 3,
        "goalDifference": 1,
        "points": 2
      }
    ]
  },
  {
    "groupId": "uuid-group-b",
    "groupName": "B",
    "entries": [...]
  }
]
```

**Response — `200 OK` (no results yet)** — all entries have 0 for all stats, ranked in registration order.

---

## Domain Impact

- Rankings are **not persisted** — computed on the fly from `Match` results.
- Filtering is applied server-side using the `groups` query parameter (comma-separated group names).
