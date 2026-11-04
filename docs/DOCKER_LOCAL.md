# Local PostgreSQL and Redis

Run commands from the repository root with Docker Desktop running.

```powershell
docker compose config
docker compose pull postgres
docker compose up -d --wait postgres
docker compose ps
```

PostgreSQL is available at `127.0.0.1:5432`, with local development username/password
`shopee` / `shopee`. Databases `shopee_user` and `shopee_product` are created on the
first startup. The ICU `vi-VN` locale matches the existing local setup. Backend
Flyway migrations create the application tables when user-service and
product-service start; starting PostgreSQL alone does not create those tables.

Override `DB_USER`, `DB_PASSWORD`, or `DB_PORT` in your environment or an untracked
`.env` file when needed. Use the same values for backend service configuration.
These defaults are for local development only.

The new named volume `shopee-clone_postgres_local_data` starts with fresh data and
does not reuse the previous `shopee-clone_shopee_postgres_data` volume. Neither
deleting an image nor recreating a container deletes named-volume data. Changing
credentials or the SQL initialization script does not reinitialize an existing
volume.

Stop and start PostgreSQL without deleting data:

```powershell
docker compose stop postgres
docker compose up -d --wait postgres
```

`docker compose down` removes containers and the Compose network while retaining
named volumes. Do not add `-v` unless you intend to delete database data. Other
containers from the previous Compose configuration can keep the network in use;
do not use `--remove-orphans` when you want to preserve them.

Redis is optional because an existing `shopee-redis` may already be running. The
default startup above leaves it running. To manage Redis with this file, first
resolve any existing `shopee-redis` container name or port 6379 conflict, preserving
its data if needed, then run:

```powershell
docker compose --profile cache pull redis
docker compose --profile cache up -d --wait redis
```

The managed Redis stores AOF data in `shopee-clone_redis_local_data`.

Redis connection settings for user-service come from Config Server. Keep local
settings in one untracked file, `backend/config-repo/user-service.yml`, under
`spring.data.redis` (`host`, `port`, and `password` when Redis requires one).
Do not duplicate this configuration in service-local `application.properties`
files. Leave the password unset when using the optional Compose Redis above,
which does not require authentication.

Config Server loads bundled `classpath:/config-repo` defaults first, then local
`file:./config-repo/` and `file:../config-repo/` overrides. Start Config Server
with its working directory set to `backend` or `backend/config-server`; both
layouts support the same `backend/config-repo` local directory. Restart Config
Server after changing its search locations, then restart user-service to load
the centralized Redis settings. The Config Server endpoint
`http://localhost:8888/user-service/default` can be used to verify the local
property source is loaded; inspect source names or individual nonsecret values
without printing or sharing passwords or the full response.

For startup errors, inspect:

```powershell
docker compose logs --tail 100 postgres
docker compose exec postgres psql -U shopee -d postgres -c '\l'
```

If port 5432 is occupied, stop the conflicting local database or set `DB_PORT` to
an available port and give the same port to the backend. If the SQL mount fails,
verify `infra/postgres/init-databases.sql` exists as a file, not a directory. The
bind mount disables automatic creation of a missing host path so a missing script
fails early. A previously initialized volume skips initialization scripts; use
the existing databases or explicitly plan a reset rather than deleting volumes
to troubleshoot a mount error.
