# URL shortener

Monorepo: **Spring Boot** (API + redirect), **Next.js** (UI), **PostgreSQL** on the **host** (local install), **Redis** via **Docker Compose**, optional **full stack** in Docker (backend + frontend containers still use your host Postgres).

## Prerequisites

- **PostgreSQL** running locally (default: `localhost:5432`). Create a database and role matching [`.env.example`](.env.example), or set `DB_*` to match your install.
- **Docker** for **Redis** (and optional Compose profile for backend/frontend). Integration tests using Testcontainers also need Docker.
- **JDK 21+** (full JDK, not a JRE-only install) and **Maven**, or use the included **`backend/mvnw`** wrapper.
- On macOS, if Maven reports *“No compiler is provided”*, your `JAVA_HOME` may point at an old JRE (e.g. browser plugin). Unset it or set it to a JDK, for example:  
  `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`  
  or the Homebrew OpenJDK path under `/opt/homebrew/opt/openjdk`.
- **Node.js 20+** and npm (for the frontend).

Copy [`.env.example`](.env.example) to `.env` (optional) and export the same variables when running the backend, or configure your IDE/run config.

## Database setup (local PostgreSQL)

Ensure Postgres is listening on the host (e.g. `5432`). Example:

```bash
createuser urlshortener
createdb -O urlshortener urlshortener
psql -d urlshortener -c "ALTER USER urlshortener WITH PASSWORD 'urlshortener';"
```

Adjust names/passwords and set `DB_NAME`, `DB_USER`, `DB_PASSWORD` accordingly. **Flyway** applies migrations when the backend starts.

## Run Redis (Docker)

```bash
docker compose up -d redis
```

Redis is available at **`localhost:6379`** (override with `REDIS_PORT` if needed).

## Run the backend locally

From `backend/` with **Postgres** and **Redis** available:

```bash
env -u JAVA_HOME ./mvnw spring-boot:run
```

Defaults: JDBC `jdbc:postgresql://localhost:5432/urlshortener` with user/password `urlshortener`, Redis `localhost:6379`. Override with `DB_*`, `REDIS_*`, etc. (see [`.env.example`](.env.example)).

- **Create short link**: `POST /api/v1/urls` with JSON `{ "longUrl": "https://example.com" }`.
- **Resolve**: `GET /r/{shortCode}` → `302` to the stored URL (Redis-first).
- **Metadata** (optional): `GET /api/v1/urls/{shortCode}`.
- **Health**: `GET /actuator/health`.

`PUBLIC_SHORT_LINK_BASE` (no trailing slash) is used to build `shortUrl` in responses, e.g. `http://localhost:8080`.

## Run the frontend locally

From `frontend/`:

```bash
export NEXT_PUBLIC_API_URL=http://localhost:8080
npm run dev
```

Open `http://localhost:3000`.

## Full stack in Docker (optional)

Runs **backend** and **frontend** containers; **PostgreSQL stays on your host**. Compose sets `DB_HOST=host.docker.internal` so the backend can reach local Postgres on macOS/Windows, with `extra_hosts: host-gateway` for Linux-friendly resolution.

```bash
docker compose --profile full up --build
```

On **Linux**, if the container cannot reach Postgres, set `DB_HOST` to your host’s address on the Docker bridge (e.g. `172.17.0.1`) or ensure `host.docker.internal` is defined for your Docker setup.

Backend: `http://localhost:8080`, frontend: `http://localhost:3000`. The browser still calls the API at `http://localhost:8080` by default.

## Tests

Backend (unit + WebMvc; integration runs only if Docker is available):

```bash
cd backend && env -u JAVA_HOME ./mvnw test
```

Frontend:

```bash
cd frontend && npm run build
```

## Design notes

- Short codes are **Base62** encodings of the numeric primary key; **unique** indexes on `short_code` and `long_url` support fast redirect lookups and **idempotent** creates (same long URL → same short code).
- Redirect path uses **Redis** with a configurable TTL; Postgres remains authoritative.
