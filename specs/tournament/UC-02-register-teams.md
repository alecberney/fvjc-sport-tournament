# UC-02 — Register Teams

## Summary

From the tournament main page, the admin registers one or multiple teams into a `DRAFT` tournament. Teams registered in the same batch are linked to the same organisation (same responsible person). Each team has an individual paid flag. The admin can also view the team list, edit team data, and delete a team.

---

## Actor

Admin

---

## Preconditions

- Tournament exists and is in `DRAFT` status.

---

## Domain Concepts

### Organisation

When one or more teams are registered in a single operation, they share an **Organisation**. An organisation tracks that these teams were registered together by the same responsible person.

| Field                   | Type   |
|-------------------------|--------|
| `id`                    | UUID   |
| `responsibleFirstName`  | String |
| `responsibleLastName`   | String |
| `tournamentId`          | UUID   |

### Team

| Field            | Type    | Description                              |
|------------------|---------|------------------------------------------|
| `id`             | UUID    | —                                        |
| `name`           | String  | Full name (base name + counter if batch) |
| `paid`           | Boolean | Default `false`                          |
| `organisationId` | UUID    | Reference to the Organisation            |
| `tournamentId`   | UUID    | —                                        |

---

## Use Cases

### UC-02.1 — Register Team(s)

#### Main Flow

1. Admin opens the registration form on the tournament main page.
2. Admin fills in: base name, responsible person (firstName, lastName), number of teams to create, paid flag per team.
3. Admin submits the form.
4. System validates the input (see Validation Rules).
5. System creates one `Organisation` for this registration.
6. System creates N `Team` entries linked to that organisation:
   - If `count = 1`: team name = `name` as provided.
   - If `count > 1`: team names = `{name} 1`, `{name} 2`, … `{name} N`.
7. Admin sees the updated team list.

#### Validation Rules

| Rule | Error message |
|------|---------------|
| `name` is blank | "Le nom de l'équipe est obligatoire" |
| `name` exceeds 250 characters | "Le nom ne peut pas dépasser 250 caractères" |
| `responsibleFirstName` is blank | "Le prénom du responsable est obligatoire" |
| `responsibleFirstName` exceeds 100 characters | "Le prénom ne peut pas dépasser 100 caractères" |
| `responsibleLastName` is blank | "Le nom du responsable est obligatoire" |
| `responsibleLastName` exceeds 100 characters | "Le nom ne peut pas dépasser 100 caractères" |
| `count` is null | "Le nombre d'équipes est obligatoire" |
| `count` < 1 | "Le nombre d'équipes doit être d'au moins 1" |
| `paid` array length != `count` | "Le nombre de flags de paiement doit correspondre au nombre d'équipes" |
| Tournament is not in `DRAFT` | "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation" |

#### API Contract

```
POST /api/tournaments/{tournamentId}/teams
```

```json
{
  "name": "Jeunesse les charbonnières",
  "responsibleFirstName": "Jean",
  "responsibleLastName": "Dupont",
  "count": 3,
  "paid": [true, false, false]
}
```

Single team (count = 1):

```json
{
  "name": "Les Aigles",
  "responsibleFirstName": "Marie",
  "responsibleLastName": "Martin",
  "count": 1,
  "paid": [false]
}
```

**Response — Success `201 Created`**

```json
[
  {
    "id": "aaa...",
    "name": "Jeunesse les charbonnières 1",
    "paid": true,
    "organisationId": "org-uuid",
    "responsibleFirstName": "Jean",
    "responsibleLastName": "Dupont"
  },
  {
    "id": "bbb...",
    "name": "Jeunesse les charbonnières 2",
    "paid": false,
    "organisationId": "org-uuid",
    "responsibleFirstName": "Jean",
    "responsibleLastName": "Dupont"
  },
  {
    "id": "ccc...",
    "name": "Jeunesse les charbonnières 3",
    "paid": false,
    "organisationId": "org-uuid",
    "responsibleFirstName": "Jean",
    "responsibleLastName": "Dupont"
  }
]
```

**Response — Validation Error `400 Bad Request`**

```json
{
  "errors": [
    { "field": "name", "message": "Le nom de l'équipe est obligatoire" }
  ]
}
```

**Response — Tournament not in DRAFT `409 Conflict`**

```json
{
  "error": "Les équipes ne peuvent être inscrites que pour un tournoi en cours de préparation"
}
```

---

### UC-02.2 — List Registered Teams

Returns all teams registered for a tournament, grouped by organisation for display.

#### API Contract

```
GET /api/tournaments/{tournamentId}/teams
```

**Response — `200 OK`**

```json
[
  {
    "id": "aaa...",
    "name": "Jeunesse les charbonnières 1",
    "paid": true,
    "organisationId": "org-uuid",
    "responsibleFirstName": "Jean",
    "responsibleLastName": "Dupont"
  },
  {
    "id": "bbb...",
    "name": "Jeunesse les charbonnières 2",
    "paid": false,
    "organisationId": "org-uuid",
    "responsibleFirstName": "Jean",
    "responsibleLastName": "Dupont"
  }
]
```

---

### UC-02.3 — Edit Team

The admin can update a team's name, responsible person, and paid status. Editing the responsible person updates the shared organisation — all sibling teams registered in the same batch will reflect the new responsible person.

#### Preconditions

- Tournament is in `DRAFT` status.

#### Validation Rules

Same rules as UC-02.1 for `name`, `responsibleFirstName`, `responsibleLastName`.

#### API Contract

```
PUT /api/tournaments/{tournamentId}/teams/{teamId}
```

```json
{
  "name": "Jeunesse les charbonnières 1",
  "responsibleFirstName": "Jean",
  "responsibleLastName": "Dupont",
  "paid": true
}
```

**Response — `200 OK`** — returns the updated `TeamDto`.

**Response — Not Found `404`** — team or tournament does not exist.

**Response — Tournament not in DRAFT `409 Conflict`**

---

### UC-02.4 — Delete Team

The admin removes a team from the tournament. If the deleted team was the last one in its organisation, the organisation is also deleted.

#### Preconditions

- Tournament is in `DRAFT` status.

#### API Contract

```
DELETE /api/tournaments/{tournamentId}/teams/{teamId}
```

**Response — `204 No Content`**

**Response — Not Found `404`**

**Response — Tournament not in DRAFT `409 Conflict`**

---

### UC-02.5 — Mark Team as Paid

The admin marks a team as paid. This can be done regardless of tournament status.

#### API Contract

```
PATCH /api/tournaments/{tournamentId}/teams/{teamId}/paid
```

```json
{
  "paid": true
}
```

**Response — `200 OK`** — returns the updated `TeamDto`.

**Response — Not Found `404`**

---

## Domain Impact

- `Organisation` is a new aggregate with its own `OrganisationStore`.
- `Team` references `OrganisationId` and `TournamentId`.
- On batch registration: one `Organisation` created, N `Team` entries created.
- On team deletion: if no remaining teams reference the `Organisation`, the `Organisation` is deleted.
- On team edit: if responsible person changes, the shared `Organisation` is updated — all sibling teams in the same organisation reflect the new responsible person.
