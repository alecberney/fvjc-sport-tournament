# FVJC Sport Tournament

Sport tournament management app for FVJC. Runs as a single JAR with SQLite, serves an Angular frontend.

## Install

The app ships as a single self-contained JAR — no separate frontend or database setup needed.

1. **Install Java 21+** — download it from [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=21).
2. **Download the app JAR** — grab `tournament-0.1.0.jar` from the [`dist/`](./dist) folder, or from the [latest release](https://github.com/alecberney/fvjc-sport-tournament/releases/latest).
3. **Run it:**

   ```bash
   java -jar tournament-0.1.0.jar
   ```

App available at http://localhost:8081

## Contribute

### Prerequisites

- Java 21+
- Node.js 20+
- Maven 3.8+

### Run as a JAR

Build and run in one command:

```bash
cd backend && mvn package && java -jar target/tournament-0.1.0.jar
```

App available at http://localhost:8081

The JAR is at `backend/target/tournament-0.1.0.jar`. `mvn package` builds the Angular frontend and bundles it inside the JAR automatically.

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
