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

## Stop

```bash
cd infra
docker compose -p booking-core down
```

## Notes

- Backend runs with `SPRING_PROFILES_ACTIVE=prod`.
- Docker uses MySQL + Redis; configure secrets via environment variables when deploying beyond local demo.
- Frontend proxies `/api/*` to backend container.
