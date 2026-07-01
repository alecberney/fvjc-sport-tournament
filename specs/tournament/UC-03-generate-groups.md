# UC-03 — Generate Groups

## Summary

The admin triggers a group draw for a `DRAFT` tournament by providing a target group size. The system computes the group distribution, runs a randomised draw that tries to avoid placing same-organisation teams in the same group, and produces named groups (A, B, C…). The admin can then manually swap teams between groups or trigger a full reshuffle.

---

## Actor

Admin

---

## Preconditions

- Tournament exists and is in `DRAFT` status.
- At least `groupSize` teams are registered.

---

## Domain Concepts

### Group

| Field          | Type        | Description                          |
|----------------|-------------|--------------------------------------|
| `id`           | UUID        | —                                    |
| `name`         | String      | Letter-based: A, B, C, …            |
| `tournamentId` | UUID        | —                                    |
| `teams`        | List\<UUID\>| Ordered list of team IDs in this group |

---

## Group Distribution Algorithm

Given `totalTeams` and `groupSize` (provided by admin):

```
numberOfGroups = floor(totalTeams / groupSize)
remainder      = totalTeams % groupSize
```

- `remainder` groups will have `groupSize + 1` teams.
- `numberOfGroups - remainder` groups will have `groupSize` teams.
- Groups never have fewer teams than `groupSize`.

**Example — 30 teams, groupSize = 4:**
```
numberOfGroups = floor(30 / 4) = 7
remainder      = 30 % 4 = 2
→ 2 groups of 5  +  5 groups of 4
```

**Example — 30 teams, groupSize = 5:**
```
numberOfGroups = 6, remainder = 0
→ 6 groups of 5
```

---

## Draw Algorithm

Goal: spread same-organisation teams across different groups as much as possible.

1. Sort organisations by number of registered teams descending.
2. For each organisation (largest first), assign each of its teams to a different group in round-robin order (Group A, B, C, …, back to A if more teams than groups).
3. Collect all remaining unassigned teams and shuffle them randomly.
4. Fill remaining slots in each group with the shuffled teams.

> If an organisation has more teams than there are groups, it is impossible to avoid same-organisation co-placement. The algorithm does its best to spread them but does not fail.

---

## Use Cases

### UC-03.1 — Compute Group Distribution (Preview)

Returns the computed group layout without persisting anything. Lets the admin confirm the distribution before generating.

#### API Contract

```
POST /api/tournaments/{tournamentId}/groups/compute
```

```json
{ "groupSize": 4 }
```

**Response — `200 OK`**

```json
{
  "numberOfGroups": 7,
  "groupsOfBaseSize": 5,
  "groupsOfBaseSizePlusOne": 2,
  "baseSize": 4,
  "totalTeams": 30
}
```

**Response — Validation Error `400 Bad Request`**

```json
{
  "errors": [
    { "field": "groupSize", "message": "La taille du groupe doit être d'au moins 2" }
  ]
}
```

---

### UC-03.2 — Generate (or Regenerate) Groups

Runs the draw and persists the groups. This endpoint serves both initial generation and reshuffle — if groups already exist for this tournament, they are fully deleted and replaced. The admin can provide a different `groupSize` on every call.

#### Main Flow

1. Admin reviews the distribution (UC-03.1) and confirms.
2. Admin triggers generation (or re-triggers with new parameters to reshuffle).
3. System deletes any existing groups for the tournament.
4. System runs the draw algorithm with the provided `groupSize`.
5. System creates groups named A, B, C, … and assigns teams.
6. Admin sees the updated group list with teams.

#### Validation Rules

| Rule | Error message |
|------|---------------|
| `groupSize` is null | "La taille du groupe est obligatoire" |
| `groupSize` < 2 | "La taille du groupe doit être d'au moins 2" |
| `totalTeams` < `groupSize` | "Pas assez d'équipes pour former un groupe" |
| Tournament not in `DRAFT` | "Les groupes ne peuvent être générés que pour un tournoi en cours de préparation" |

#### API Contract

```
POST /api/tournaments/{tournamentId}/groups/generate
```

```json
{ "groupSize": 4 }
```

**Response — `201 Created`**

```json
[
  {
    "id": "uuid-a",
    "name": "A",
    "teams": [
      { "id": "uuid-t1", "name": "Les Aigles", "organisationId": "org-1", "paid": true },
      { "id": "uuid-t2", "name": "Jeunesse les charbonnières 1", "organisationId": "org-2", "paid": false },
      { "id": "uuid-t3", "name": "Les Faucons", "organisationId": "org-3", "paid": false },
      { "id": "uuid-t4", "name": "Les Loups", "organisationId": "org-4", "paid": true }
    ]
  },
  {
    "id": "uuid-b",
    "name": "B",
    "teams": [...]
  }
]
```

**Response — Validation Error `400 Bad Request`**

```json
{
  "errors": [
    { "field": "groupSize", "message": "Pas assez d'équipes pour former un groupe" }
  ]
}
```

**Response — Tournament not in DRAFT `409 Conflict`**

---

### UC-03.3 — Get Groups

Returns the current group draw for a tournament.

#### API Contract

```
GET /api/tournaments/{tournamentId}/groups
```

**Response — `200 OK`** — same structure as UC-03.2 response.

**Response — `200 OK` (no groups yet)**

```json
[]
```

---

### UC-03.4 — Swap Two Teams

The admin manually swaps two teams between two different groups. Both teams move to the other's group.

#### Preconditions

- Tournament is in `DRAFT` status.
- Both teams belong to the same tournament.
- The two teams are in different groups.

#### Validation Rules

| Rule | Error message |
|------|---------------|
| Either team not found | "Équipe introuvable" |
| Teams are in the same group | "Les deux équipes sont déjà dans le même groupe" |
| Tournament not in `DRAFT` | "Les groupes ne peuvent être modifiés que pour un tournoi en cours de préparation" |

#### API Contract

```
POST /api/tournaments/{tournamentId}/groups/swap
```

```json
{
  "teamId1": "uuid-t1",
  "teamId2": "uuid-t5"
}
```

**Response — `200 OK`** — returns both affected groups after the swap.

```json
[
  {
    "id": "uuid-a",
    "name": "A",
    "teams": [...]
  },
  {
    "id": "uuid-b",
    "name": "B",
    "teams": [...]
  }
]
```

**Response — Validation Error `400 Bad Request`**

**Response — Tournament not in DRAFT `409 Conflict`**

---

## Domain Impact

- `Group` is a new aggregate with its own `GroupStore`.
- A `Group` holds an ordered list of `TeamId` references — teams are not embedded, just referenced.
- Generating groups (UC-03.2) deletes all existing `Group` entries for the tournament and creates new ones — this is a destructive operation.
- Swapping (UC-03.4) updates the team lists of two existing `Group` entries.
- Groups are named sequentially with uppercase letters: A, B, C, …, Z, AA, AB, … (standard alphabetical continuation if more than 26 groups).
