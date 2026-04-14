# booking-core docker-compose

## Services

- `frontend`: Nginx static hosting for Vite build (`http://localhost:25173`)
- `backend`: Spring Boot API (`http://localhost:28080/api`)
- `redis`: cache store (`localhost:26379`)

## Start

```bash
cd infra
pwsh -ExecutionPolicy Bypass -File .\start.ps1

# Optional:
# pwsh -ExecutionPolicy Bypass -File .\start.ps1 -Rebuild:$false
# pwsh -ExecutionPolicy Bypass -File .\start.ps1 -Detach:$false
```

`start.ps1` is now the unified entrypoint:

- Full stack (default): `pwsh -ExecutionPolicy Bypass -File .\start.ps1`
- Frontend only: `pwsh -ExecutionPolicy Bypass -File .\start.ps1 -Mode frontend`

## Stop

```bash
cd infra
docker compose -p booking-core down
```

## Frontend only (local deploy)

```bash
cd infra
pwsh -ExecutionPolicy Bypass -File .\start.ps1 -Mode frontend
```

Open `http://localhost:18080`, then verify:

```bash
curl http://localhost:18080/healthz
```

Stop:

```bash
cd infra
docker compose -p booking-core -f docker-compose.frontend.yml down
```

## Notes

- Backend runs with `SPRING_PROFILES_ACTIVE=prod`.
- Docker uses MySQL + Redis; configure secrets via environment variables when deploying beyond local demo.
- Frontend proxies `/api/*` to backend container.
- This repo currently uses backend MySQL in dev/prod-like runs; if you switch backend to H2 in-memory for quick tests, data is ephemeral and resets on restart.
