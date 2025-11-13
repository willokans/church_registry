# Church Registry

A production-ready multi-tenant backend service for managing Catholic sacramental registers in Nigeria and similar regions with spotty internet connectivity.

## Features

- **Multi-tenancy**: Tenant isolation via hostname or header-based resolution
- **RBAC**: Role-based access control with permissions mapping
- **Soft Delete**: All deletions are soft (ACTIVE/INACTIVE status)
- **Audit Logging**: Append-only audit logs with optional hash chaining
- **Idempotency**: Safe retries via Idempotency-Key header
- **OAuth2/JWT**: Resource server with OIDC token validation
- **Storage**: S3-compatible storage abstraction (MinIO in dev, GCS/S3 in prod)
- **Observability**: Spring Actuator, Micrometer, OpenTelemetry, structured JSON logs
- **ETag Support**: Conditional requests with If-None-Match
- **Cursor Pagination**: Efficient pagination for large datasets

## Tech Stack

- **Language**: Kotlin 1.9+
- **Framework**: Spring Boot 3.3+ (AOT enabled)
- **Build**: Maven
- **Database**: PostgreSQL 15+
- **Migrations**: Liquibase
- **Auth**: OAuth2 Resource Server (JWT) with OIDC
- **Storage**: MinIO (dev) / S3-compatible (prod)
- **Testing**: JUnit 5, Testcontainers

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (for local development)
- PostgreSQL 15+ (or use Docker Compose)

## Quick Start

### 1. Clone and Setup

```bash
git clone <repository-url>
cd church_registry
```

### 2. Environment Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Key environment variables:
- `JWT_ISSUER_URI`: Your OIDC issuer URI (e.g., Auth0, Google Identity Platform)
- `JWT_AUDIENCE`: Expected JWT audience
- `DB_URL`: PostgreSQL connection string
- `S3_ENDPOINT`: MinIO or S3 endpoint
- `S3_ACCESS_KEY` / `S3_SECRET_KEY`: Storage credentials

### 3. Run with Docker Compose

```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- MinIO on ports 9000 (API) and 9001 (Console)
- Application on port 8080

### 4. Run Locally (without Docker)

```bash
# Start PostgreSQL and MinIO via Docker Compose
docker-compose up -d postgres minio

# Run the application
./mvnw spring-boot:run
```

### 5. Verify

```bash
# Health check
curl http://localhost:8080/actuator/health

# Public homepage (sample tenant)
curl http://localhost:8080/public/sample-parish/home
```

## API Documentation

When running in dev mode, Swagger UI is available at:
- URL: `http://localhost:8080/swagger-ui.html`
- Basic Auth: Configured via `OPENAPI_BASIC_AUTH_USER` and `OPENAPI_BASIC_AUTH_PASSWORD`

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/registry` |
| `DB_USER` | Database user | `registry` |
| `DB_PASSWORD` | Database password | `registry` |
| `JWT_ISSUER_URI` | OIDC issuer URI | Required |
| `JWT_AUDIENCE` | JWT audience | `registry-api` |
| `S3_ENDPOINT` | S3 endpoint | `http://localhost:9000` |
| `S3_BUCKET` | S3 bucket name | `registry-attachments` |
| `S3_ACCESS_KEY` | S3 access key | `minio` |
| `S3_SECRET_KEY` | S3 secret key | `minio123` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry endpoint | Optional |
| `SENTRY_DSN` | Sentry DSN | Optional |
| `CORS_ALLOWED_ORIGINS` | CORS origins | `http://localhost:3000,http://localhost:8080` |
| `TENANT_RESOLUTION_MODE` | `header` or `hostname` | `header` |
| `TENANT_HEADER_NAME` | Tenant header name | `X-Tenant` |
| `STORAGE_TYPE` | `s3` or `local` | `s3` |
| `STORAGE_LOCAL_BASE_PATH` | Local storage base path | `./storage` |
| `STORAGE_LOCAL_BASE_URL` | Local storage base URL | `http://localhost:8080/storage` |
| `RATE_LIMITING_ENABLED` | Enable rate limiting | `false` |
| `PORT` | Server port (Cloud Run) | `8080` |

## API Authentication

The API uses OAuth2 Resource Server with JWT tokens. Include the token in the Authorization header:

```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     -H "X-Tenant: sample-parish" \
     http://localhost:8080/admin/users
```

### Tenant Resolution

Tenants can be resolved via:
1. **Header** (default): `X-Tenant: <tenant-slug>`
2. **Hostname**: `https://<tenant-slug>.yourdomain.com`

## Database Migrations

Liquibase manages database schema. Migrations are in `src/main/resources/db/changelog/`.

### Apply Migrations

Migrations run automatically on startup. To validate:

```bash
./mvnw liquibase:validate
```

### Manual Migration

```bash
./mvnw liquibase:update
```

## Testing

### Run Tests

```bash
# All tests
./mvnw test

# Integration tests only
./mvnw test -Dtest=*IntegrationTest

# Unit tests only
./mvnw test -Dtest=*Test
```

### Testcontainers

Integration tests use Testcontainers for PostgreSQL. Ensure Docker is running.

## Building

### Build JAR

```bash
./mvnw clean package
```

### Build Docker Image

```bash
# Using Jib
./mvnw jib:build

# Using Dockerfile
docker build -t church-registry:latest .
```

## Deployment

### Google Cloud Run

1. Build and push image:
```bash
./mvnw jib:build -Dimage=gcr.io/YOUR_PROJECT/church-registry:latest
```

2. Deploy:
```bash
gcloud run deploy church-registry \
  --image gcr.io/YOUR_PROJECT/church-registry:latest \
  --platform managed \
  --region us-central1 \
  --set-env-vars JWT_ISSUER_URI=...,DB_URL=...,etc
```

3. **Cloud SQL Connection**: Use Cloud SQL Proxy or Unix socket:
   - Set `DB_URL=jdbc:postgresql:///registry?cloudSqlInstance=PROJECT:REGION:INSTANCE&socketFactory=com.google.cloud.sql.postgres.SocketFactory`

### Docker Compose (Production-like)

```bash
docker-compose -f compose.yaml up -d
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/com/example/registry/
│   │   ├── config/          # Configuration classes
│   │   ├── domain/          # Domain entities and enums
│   │   ├── repo/            # Spring Data repositories
│   │   ├── security/        # Security and authorization
│   │   ├── service/          # Business logic services
│   │   ├── storage/         # Storage abstraction
│   │   ├── pdf/             # PDF generation SPI
│   │   ├── tenancy/         # Multi-tenancy support
│   │   ├── util/            # Utilities
│   │   └── web/             # Controllers, DTOs, exception handlers
│   └── resources/
│       ├── application.yml  # Main configuration
│       └── db/changelog/    # Liquibase migrations
└── test/
    └── kotlin/              # Tests
```

## Roles and Permissions

### Roles

- `SUPER_ADMIN`: Full access to all tenants
- `PARISH_ADMIN`: Administrative access within a tenant
- `REGISTRAR`: Can create and update sacramental records
- `PRIEST`: Can create sacramental records
- `VIEWER`: Read-only access

### Permissions

- `users.manage`: Manage users
- `users.view`: View users
- `permissions.grant`: Grant roles/permissions
- `sacraments.create`: Create sacramental events
- `sacraments.update`: Update sacramental events
- `sacraments.view`: View sacramental events
- `settings.edit`: Edit tenant settings
- `audit.view`: View audit logs

## API Endpoints

### Public

- `GET /public/{tenantSlug}/home` - Public homepage content

### Admin

- `GET /admin/users` - List users (requires `users.manage`)
- `POST /admin/users/invite` - Invite user (requires `users.manage`)
- `POST /admin/users/{id}/role` - Update user role (requires `permissions.grant`)
- `POST /admin/users/{id}/status` - Update user status (requires `users.manage`)
- `POST /admin/content/{key}/publish` - Publish content block (requires `settings.edit`)

### Sacraments

- `GET /sacraments` - List sacramental events (cursor pagination)
- `POST /sacraments` - Create event (requires `sacraments.create`, needs `Idempotency-Key`)
- `PUT /sacraments/{id}` - Update event (requires `sacraments.update`)
- `POST /sacraments/{id}/status` - Update status (requires `sacraments.update`)

### Certificates

- `GET /certificates/{serial}/verify` - Verify certificate (public)

### Audit

- `GET /audit` - Query audit logs (requires `audit.view`)

### User Profile

- `GET /me` - Get current user profile and memberships

## Observability

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

### Logs

Logs are structured JSON in production. In dev, they're formatted for readability.

## Troubleshooting

### Database Connection Issues

- Verify PostgreSQL is running: `docker-compose ps`
- Check connection string in `.env`
- Ensure migrations have run: Check Liquibase changelog lock table

### JWT Validation Issues

- Verify `JWT_ISSUER_URI` points to valid OIDC discovery endpoint
- Check JWT audience matches `JWT_AUDIENCE`
- Ensure token includes `sub` claim (user ID)

### Tenant Resolution Issues

- Verify tenant exists in database
- Check `X-Tenant` header is set (if using header mode)
- Verify tenant slug matches database

## License

[Your License Here]

## Contributing

[Contributing Guidelines]

