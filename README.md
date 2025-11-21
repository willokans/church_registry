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
- **Database**: PostgreSQL 15+ (production) / H2 (local development)
- **Migrations**: Liquibase
- **Auth**: OAuth2 Resource Server (JWT) with OIDC
- **Storage**: MinIO (dev) / S3-compatible (prod)
- **Testing**: JUnit 5, Testcontainers, Mockito

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose (for local development with PostgreSQL)
- PostgreSQL 15+ (optional, can use H2 for local dev)

## Quick Start

### Option 1: Run with H2 (No Docker Required)

The easiest way to get started is using H2 in-memory database:

```bash
./run-h2.sh
```

This will:

- Start the application with H2 in-memory database
- Use local filesystem storage (no MinIO needed)
- Create dummy users for testing
- Access at `http://localhost:8080`

**H2 Console Access:**

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:registry`
- Username: `sa`
- Password: (empty)

### Option 2: Run with Docker Compose (PostgreSQL + MinIO)

```bash
docker-compose up -d
```

This starts:

- PostgreSQL on port 5432
- MinIO on ports 9000 (API) and 9001 (Console)
- Application on port 8080

### Option 3: Run Locally (without Docker)

```bash
# Start PostgreSQL and MinIO via Docker Compose
docker-compose up -d postgres minio

# Run the application
./mvnw spring-boot:run
```

### Verify Installation

```bash
# Health check
curl http://localhost:8080/actuator/health

# Public homepage (sample tenant)
curl http://localhost:8080/api/public/sample-parish/home
```

## Security Implementation

### Authentication & Authorization

The application uses **OAuth2 Resource Server** with JWT tokens for authentication. The security implementation includes:

#### Production Mode (OAuth2/JWT)

- Validates JWT tokens from OIDC issuer
- Extracts user information from JWT claims (`sub`, `email`, `roles`)
- Uses Spring Security's `BearerTokenAuthenticationFilter` for token validation

#### H2/Development Mode (Mock Authentication)

For local development without an OAuth2 provider, the application includes:

1. **H2SecurityConfig**: Mock JWT decoder that accepts email-based tokens

   - Accepts tokens like `Bearer super-admin@test.com`
   - Extracts role from email pattern (e.g., `super-admin@test.com` → `SUPER_ADMIN`)
   - Creates mock JWT with proper claims

2. **H2AuthenticationFilter**: Custom filter that intercepts email-based tokens

   - Runs before `BearerTokenAuthenticationFilter`
   - Processes tokens containing `@` symbol (email format)
   - Sets authentication in SecurityContext
   - Removes Authorization header to prevent double processing

3. **JwtAuthenticationConverter**: Converts JWT to Spring Security Authentication
   - Extracts roles from JWT `roles` claim
   - Adds `ROLE_` prefixed authorities

#### Security Configuration

- **CSRF**: Disabled (stateless API)
- **Session**: Stateless (no session storage)
- **CORS**: Configurable via `CORS_ALLOWED_ORIGINS`
- **Public Endpoints**:
  - `/actuator/health`, `/actuator/info`
  - `/api/public/**`
  - `/api/certificates/*/verify`
  - `/h2-console/**`

#### Role-Based Access Control (RBAC)

Roles are extracted from JWT and mapped to permissions:

- **SUPER_ADMIN**: Full access to all tenants
- **PARISH_ADMIN**: Administrative access within a tenant
- **REGISTRAR**: Can create and update sacramental records
- **PRIEST**: Can create sacramental records
- **VIEWER**: Read-only access

Permissions are checked via `@PreAuthorize` annotations and `AuthorizationService`.

### Dummy Users for Testing (H2 Mode)

When running with H2 profile, the following test users are automatically created:

| Email                   | Role         | Description               |
| ----------------------- | ------------ | ------------------------- |
| `super-admin@test.com`  | SUPER_ADMIN  | Full system access        |
| `parish-admin@test.com` | PARISH_ADMIN | Parish administration     |
| `registrar@test.com`    | REGISTRAR    | Can create/update records |
| `priest@test.com`       | PRIEST       | Can create records        |
| `viewer@test.com`       | VIEWER       | Read-only access          |

All users are granted membership to the default tenant.

## Using Postman

### Setup

1. **Base URL**: `http://localhost:8080`
2. **Authentication**: Use email-based tokens in H2 mode

### Authentication Headers

For H2/local development, use email-based tokens:

```
Authorization: Bearer super-admin@test.com
```

For production, use real JWT tokens:

```
Authorization: Bearer <your-jwt-token>
```

### Tenant Resolution

Add tenant header for multi-tenant endpoints:

```
X-Tenant: sample-parish
```

### Example Requests

#### 1. Get All Tenants (SUPER_ADMIN required)

```http
GET http://localhost:8080/api/admin/tenants
Authorization: Bearer super-admin@test.com
```

#### 2. Create Tenant

```http
POST http://localhost:8080/api/admin/tenants
Authorization: Bearer super-admin@test.com
Content-Type: application/json

{
  "slug": "st-marys-parish",
  "name": "St. Mary's Parish",
  "parentId": null,
  "theme": {
    "primaryColor": "#0066cc",
    "secondaryColor": "#ffffff",
    "logo": "/logo.png"
  }
}
```

#### 3. Update Tenant

```http
PUT http://localhost:8080/api/admin/tenants/1
Authorization: Bearer super-admin@test.com
Content-Type: application/json

{
  "name": "St. Mary's Parish - Updated",
  "theme": {
    "primaryColor": "#ff0000"
  }
}
```

#### 4. Get Current User Profile

```http
GET http://localhost:8080/api/admin/users/me
Authorization: Bearer super-admin@test.com
X-Tenant: sample-parish
```

#### 5. Create Sacrament Event (requires Idempotency-Key)

```http
POST http://localhost:8080/api/sacraments
Authorization: Bearer registrar@test.com
X-Tenant: sample-parish
Idempotency-Key: unique-key-123
Content-Type: application/json

{
  "type": "BAPTISM",
  "personId": "550e8400-e29b-41d4-a716-446655440000",
  "date": "2025-01-15",
  "ministerId": null,
  "bookNo": 1,
  "pageNo": 1,
  "entryNo": 1
}
```

#### 6. Get Sacraments (with pagination)

```http
GET http://localhost:8080/api/sacraments?cursor=10&limit=20
Authorization: Bearer viewer@test.com
X-Tenant: sample-parish
```

#### 7. Verify Certificate (Public, no auth required)

```http
GET http://localhost:8080/api/certificates/ABC123/verify
```

### Postman Collection Setup

1. Create a new collection
2. Add environment variables:
   - `base_url`: `http://localhost:8080`
   - `token`: `super-admin@test.com` (for H2 mode)
3. Set collection-level headers:
   - `Authorization`: `Bearer {{token}}`
   - `Content-Type`: `application/json`
   - `X-Tenant`: `sample-parish` (for multi-tenant endpoints)

## API Documentation

All API endpoints are prefixed with `/api`. When running in dev mode, Swagger UI is available at:

- URL: `http://localhost:8080/swagger-ui.html`
- Basic Auth: Configured via `OPENAPI_BASIC_AUTH_USER` and `OPENAPI_BASIC_AUTH_PASSWORD`

### API Endpoints

#### Public Endpoints

- `GET /api/public/{tenantSlug}/home` - Get public homepage content (no auth required)

#### Tenant Management (`/api/admin/tenants`)

- `GET /api/admin/tenants` - List all tenants (SUPER_ADMIN)
- `GET /api/admin/tenants/{id}` - Get tenant by ID (SUPER_ADMIN)
- `GET /api/admin/tenants/slug/{slug}` - Get tenant by slug
- `POST /api/admin/tenants` - Create tenant
- `PUT /api/admin/tenants/{id}` - Update tenant (SUPER_ADMIN)

#### User Management (`/api/admin/users`)

- `GET /api/admin/users/me` - Get current user profile
- `GET /api/admin/users` - List users (requires `users.manage`)
- `POST /api/admin/users/invite` - Invite user (requires `users.manage`)
- `POST /api/admin/users/{id}/role` - Update user role (requires `permissions.grant`)
- `POST /api/admin/users/{id}/status` - Update user status (requires `users.manage`)

#### Sacrament Events (`/api/sacraments`)

- `GET /api/sacraments` - List sacramental events (cursor pagination, supports filters)
- `POST /api/sacraments` - Create event (requires `sacraments.create`, needs `Idempotency-Key`)
- `PUT /api/sacraments/{id}` - Update event (requires `sacraments.update`)
- `POST /api/sacraments/{id}/status` - Update status (requires `sacraments.update`)

#### Certificates (`/api/certificates`)

- `GET /api/certificates/{serial}/verify` - Verify certificate (public, no auth required)

#### Content Management (`/api/admin/content`)

- `POST /api/admin/content/{key}/publish` - Publish content block (requires `settings.edit`)

#### Audit Logs (`/api/audit`)

- `GET /api/audit` - Query audit logs (requires `audit.view`, supports cursor pagination)

## Environment Variables

| Variable                      | Description             | Default                                       |
| ----------------------------- | ----------------------- | --------------------------------------------- |
| `SERVER_PORT`                 | Server port             | `8080`                                        |
| `SPRING_PROFILES_ACTIVE`      | Active profile          | `dev`                                         |
| `DB_URL`                      | PostgreSQL JDBC URL     | `jdbc:postgresql://localhost:5432/registry`   |
| `DB_USER`                     | Database user           | `registry`                                    |
| `DB_PASSWORD`                 | Database password       | `registry`                                    |
| `JWT_ISSUER_URI`              | OIDC issuer URI         | Required (not needed for H2 mode)             |
| `JWT_AUDIENCE`                | JWT audience            | `registry-api`                                |
| `S3_ENDPOINT`                 | S3 endpoint             | `http://localhost:9000`                       |
| `S3_BUCKET`                   | S3 bucket name          | `registry-attachments`                        |
| `S3_ACCESS_KEY`               | S3 access key           | `minio`                                       |
| `S3_SECRET_KEY`               | S3 secret key           | `minio123`                                    |
| `STORAGE_TYPE`                | `s3` or `local`         | `s3`                                          |
| `STORAGE_LOCAL_BASE_PATH`     | Local storage base path | `./storage`                                   |
| `STORAGE_LOCAL_BASE_URL`      | Local storage base URL  | `http://localhost:8080/storage`               |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OpenTelemetry endpoint  | Optional                                      |
| `SENTRY_DSN`                  | Sentry DSN              | Optional                                      |
| `CORS_ALLOWED_ORIGINS`        | CORS origins            | `http://localhost:3000,http://localhost:8080` |
| `TENANT_RESOLUTION_MODE`      | `header` or `hostname`  | `header`                                      |
| `TENANT_HEADER_NAME`          | Tenant header name      | `X-Tenant`                                    |
| `RATE_LIMITING_ENABLED`       | Enable rate limiting    | `false`                                       |
| `PORT`                        | Server port (Cloud Run) | `8080`                                        |

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
- For H2 mode: Check `run-h2.sh` script

### JWT Validation Issues

- **H2 Mode**: Use email-based tokens like `Bearer super-admin@test.com`
- **Production**: Verify `JWT_ISSUER_URI` points to valid OIDC discovery endpoint
- Check JWT audience matches `JWT_AUDIENCE`
- Ensure token includes `sub` claim (user ID)

### Tenant Resolution Issues

- Verify tenant exists in database
- Check `X-Tenant` header is set (if using header mode)
- Verify tenant slug matches database

### CSRF Errors (403 Forbidden)

- CSRF is disabled for stateless API
- If you see CSRF errors, check that `H2AuthenticationFilter` is running before `BearerTokenAuthenticationFilter`
- Verify `http.csrf { it.disable() }` is set in `SecurityConfig`

### Authentication Errors (401 Unauthorized)

- **H2 Mode**: Ensure token format is `Bearer email@test.com`
- Check that `H2AuthenticationFilter` is active (should see logs)
- Verify user exists in database (check H2 console)

## License

[Your License Here]

## Contributing

[Contributing Guidelines]
