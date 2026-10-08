---
name: start-app
description: Start the FVJC tournament app locally in dev mode (Spring Boot backend on :8081 + Angular dev server on :4200 with API proxy). Use when asked to run, start, launch or serve the app, or to check a change in the running app.
---

# Start the app (dev mode)

Two processes, each run **in the background** (Bash `run_in_background: true`) so the session stays usable:

| Process | Directory | Command | URL |
|---------|-----------|---------|-----|
| Backend (Spring Boot, SQLite) | `backend/` | `./mvnw spring-boot:run` | http://localhost:8081 |
| Frontend (Angular dev server) | `frontend/` | `npm start` | http://localhost:4200 |

The frontend proxies `/api` to `http://localhost:8081` (`frontend/proxy.conf.json`), so open **http://localhost:4200**.

## Steps

1. **See what's already running.** Don't start a second copy:
   ```bash
   lsof -nP -iTCP:8081 -sTCP:LISTEN; lsof -nP -iTCP:4200 -sTCP:LISTEN
   ```
   If a port is busy with this app, reuse it. If something else is using the port, ask the user before killing it.
2. **Prerequisites.**
   - Java 25: the repo has `.sdkmanrc` (`sdk env`). Check with `java -version`.
   - Node: the version is in `.nvmrc`. If `frontend/node_modules` is missing, run `npm ci` in `frontend/` first.
3. **Start the backend** in the background from `backend/`: `./mvnw spring-boot:run`.
   The SQLite DB is `backend/database/tournament.db` (override with `TOURNAMENT_DB_PATH`). Liquibase migrations run on startup.
4. **Start the frontend** in the background from `frontend/`: `npm start`.
5. **Wait until both are ready.** Use Monitor or an until-loop, not a `sleep`:
   ```bash
   until curl -sf -o /dev/null http://localhost:8081/api/tournaments; do sleep 2; done
   until curl -sf -o /dev/null http://localhost:4200; do sleep 2; done
   ```
   If either one fails, read its background output and report the error. Don't retry blindly.
6. Tell the user the app is up at http://localhost:4200.

## Tips

- To get test data fast: create a tournament, then open the ⚙ settings cog on its page → **"Générer des équipes aléatoires"**.
- Production-style single JAR (serves the built frontend from `static/`): `cd backend && ./mvnw package && java -jar target/tournament-*.jar`, then open http://localhost:8081.
- To stop: stop the background tasks (TaskStop), or kill the PIDs listening on 8081 and 4200.
