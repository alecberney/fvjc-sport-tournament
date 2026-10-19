# FVJC Sport Tournament

[![CI](https://github.com/alecberney/fvjc-sport-tournament/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/alecberney/fvjc-sport-tournament/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=alecberney_fvjc-sport-tournament&metric=alert_status)](https://sonarcloud.io/summary/overall?id=alecberney_fvjc-sport-tournament&branch=main)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=alecberney_fvjc-sport-tournament&metric=coverage)](https://sonarcloud.io/component_measures?id=alecberney_fvjc-sport-tournament&metric=coverage&branch=main)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=alecberney_fvjc-sport-tournament&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=alecberney_fvjc-sport-tournament&branch=main)

Sport tournament management app for FVJC. Runs as a single JAR with SQLite, serves an Angular frontend.

## Install

The app ships as a single self-contained JAR — no separate frontend or database setup needed.

1. **Install Java 25+** — download it from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=25).
2. **Download the app JAR** — grab `tournament-0.2.0.jar` from the [`dist/`](./dist) folder, or from the [latest release](https://github.com/alecberney/fvjc-sport-tournament/releases/latest).
3. **Run it:**

   ```bash
   java -jar tournament-0.2.0.jar
   ```

App available at http://localhost:8081

## Contribute

### Prerequisites

- Java 25+
- Node.js 20+
- Maven 3.8+

With [SDKMAN!](https://sdkman.io/) installed, run `sdk env init` at the repo root to create a local (git-ignored) `.sdkmanrc` pinning `java=25.0.3-tem`, then `sdk env install`. With `sdkman_auto_env=true` the JDK is selected automatically whenever you `cd` into the repo.

### Run as a JAR

Build and run in one command:

```bash
cd backend && mvn package && java -jar target/tournament-0.2.0.jar
```

App available at http://localhost:8081

The JAR is at `backend/target/tournament-0.2.0.jar`. `mvn package` builds the Angular frontend and bundles it inside the JAR automatically.

### Dev mode

Run backend and frontend separately with hot reload.

**Terminal 1 — Backend**
```bash
cd backend
mvn spring-boot:run
```

**Terminal 2 — Frontend**
```bash
cd frontend
npm start
```

Frontend at http://localhost:4200 — API calls are proxied to the backend on port 8081.

### Tests & coverage

**Backend** — JaCoCo is attached to the test run, no extra flag needed:

```bash
cd backend
./mvnw test -Dfrontend.skip=true
```

Report: `backend/target/site/jacoco/index.html` (XML for Sonar at `target/site/jacoco/jacoco.xml`).

**Frontend** — Karma with the Istanbul coverage reporter:

```bash
cd frontend
npm run test:coverage
```

Report: `frontend/coverage/frontend/index.html` (LCOV for Sonar at `coverage/frontend/lcov.info`).

**End-to-end** — Playwright drives the real app in Chrome (uses your installed Google Chrome, no browser download). With the app running in dev mode (backend on `:8081`, `npm start` on `:4200`):

```bash
cd frontend
npm run e2e          # all specs in frontend/e2e/
npm run e2e:report   # open the last HTML report
```

To target another URL (e.g. the packaged jar), set `E2E_BASE_URL=http://localhost:8081`. Each spec creates its own tournament through the API and deletes it afterwards. In CI, the `e2e` job in [`ci.yml`](./.github/workflows/ci.yml) builds the jar, starts it on a fresh SQLite database and runs the suite against it; the Playwright report is uploaded as an artifact when it fails.

Both reports are uploaded to [SonarCloud](https://sonarcloud.io/summary/overall?id=alecberney_fvjc-sport-tournament&branch=main) by the `sonar` job in [`ci.yml`](./.github/workflows/ci.yml) on every push to `main` and every pull request. Analysis settings live in [`sonar-project.properties`](./sonar-project.properties).
