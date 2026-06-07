# Running the MYDrive backend in Docker

A multi-stage `Dockerfile` is provided: it builds the Spring Boot app with Maven
(JDK 17) and runs the repackaged executable WAR on a slim JRE image as a
non-root user.

## Build

```bash
# from 02_Development/backend/MYDrive
docker build -t mydrive-backend .
```

## Run

The app needs a MySQL database and a few secrets. All settings from
`application.properties` can be overridden with environment variables via Spring
Boot's relaxed binding (e.g. `app.jwt.secret` → `APP_JWT_SECRET`).

```bash
docker run -d --name mydrive-backend -p 8083:8083 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/mydrivedb" \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  -e APP_JWT_SECRET="change-me-to-a-long-random-secret-at-least-32-chars" \
  -v mydrive_storage:/app/storage \
  -v mydrive_signing:/app/signing \
  mydrive-backend
```

> On Linux hosts, `host.docker.internal` may not resolve — point
> `SPRING_DATASOURCE_URL` at your DB host/container directly, or add
> `--add-host=host.docker.internal:host-gateway`.

### Environment variables

| Variable | Purpose | Default (in image) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL of the MySQL database | `jdbc:mysql://localhost:3306/mydrivedb` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | DB credentials | `root` / `root` |
| `APP_JWT_SECRET` | JWT signing secret (≥ 32 chars) | placeholder — **set this** |
| `APP_JWT_EXPIRATION_MS` | Access-token lifetime | `3600000` |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | SMTP creds for password-reset mail | placeholder |
| `APP_STORAGE_PATH` | On-disk file storage (volume) | `/app/storage` |
| `APP_SIGNING_KEYSTORE_PATH` | PAdES signing keystore (auto-created) | `/app/signing/mydrive-demo.p12` |
| `APP_SIGNING_KEYSTORE_PASSWORD` / `APP_SIGNING_KEY_ALIAS` | Keystore access | `mydrive-demo-pass` / `mydrive-signing` |
| `SERVER_PORT` | HTTP port | `8083` |
| `JAVA_OPTS` | Extra JVM flags, e.g. `-Xmx512m` | empty |

### Volumes

`/app/storage` (uploaded files) and `/app/signing` (signing keystore) are
declared as volumes so data survives container restarts.

### Notes

- The image exposes Actuator at `/actuator/health`. Spring Security currently
  secures it (returns 401), so the Docker `HEALTHCHECK` treats any non-5xx
  response as healthy. To get a true `"status":"UP"` check, permit
  `/actuator/health` in `SecurityConfig` and adjust the healthcheck.
- Tests are skipped during the image build for speed; remove `-DskipTests` in
  the Dockerfile's build stage to run them.
- For a quick local MySQL: `docker run -d --name mydrive-db -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=mydrivedb -p 3306:3306 mysql:8`.
