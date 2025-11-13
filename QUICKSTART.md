# Quick Start Guide

## Prerequisites

- Java 21+ installed
- Maven 3.8+ installed (or use Maven wrapper)
- Docker & Docker Compose (for database and storage)

## Option 1: Run with Docker Compose (Easiest)

This starts PostgreSQL, MinIO, and the application together:

```bash
# 1. Start all services
docker-compose up -d

# 2. Check logs
docker-compose logs -f app

# 3. Stop services
docker-compose down
```

The application will be available at: `http://localhost:8080`

## Option 2: Run Locally (Development)

### Step 1: Start Dependencies

```bash
# Start PostgreSQL and MinIO only
docker-compose up -d postgres minio

# Verify they're running
docker-compose ps
```

### Step 2: Set Environment Variables

Create a `.env` file or export variables:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/registry
export DB_USER=registry
export DB_PASSWORD=registry
export JWT_ISSUER_URI=https://your-issuer/.well-known/openid-configuration
export JWT_AUDIENCE=registry-api
export S3_ENDPOINT=http://localhost:9000
export S3_BUCKET=registry-attachments
export S3_ACCESS_KEY=minio
export S3_SECRET_KEY=minio123
```

**Note:** For local development without OAuth2, you can use a mock issuer or disable security temporarily.

### Step 3: Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/church-registry-1.0.0-SNAPSHOT.jar
```

## Option 3: Run with H2 Database (No Docker, No PostgreSQL) ⭐ EASIEST

Perfect for local development without any external dependencies:

```bash
# Option A: Use the provided script
./run-h2.sh

# Option B: Manual setup
export SPRING_PROFILES_ACTIVE=h2,dev
export STORAGE_TYPE=local
mvn spring-boot:run
```

This will:

- Use H2 in-memory database (no PostgreSQL needed)
- Use local filesystem storage (no MinIO needed)
- Auto-create schema on startup
- Access H2 console at: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:registry`
  - Username: `sa`
  - Password: (empty)

**Note:** Data is lost when the application stops (in-memory database).

## Option 4: Run with Local Storage (No MinIO)

If you have PostgreSQL but don't want MinIO:

```bash
export STORAGE_TYPE=local
export STORAGE_LOCAL_BASE_PATH=./storage
export STORAGE_LOCAL_BASE_URL=http://localhost:8080/storage

mvn spring-boot:run
```

## Verify It's Running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Public homepage (sample tenant)
curl http://localhost:8080/public/sample-parish/home

# Swagger UI (if enabled)
open http://localhost:8080/swagger-ui.html
```

## Access Points

- **Application**: http://localhost:8080
- **MinIO Console**: http://localhost:9001 (user: `minio`, password: `minio123`)
- **PostgreSQL**: localhost:5432 (user: `registry`, password: `registry`, db: `registry`)
- **Actuator Health**: http://localhost:8080/actuator/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html (dev profile only)

## Troubleshooting

### Database Connection Issues

```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Connect to database manually
docker-compose exec postgres psql -U registry -d registry
```

### Port Already in Use

```bash
# Change port
export SERVER_PORT=8081
mvn spring-boot:run
```

### JWT/OAuth2 Issues

For local development, you may need to:

1. Set up a local OAuth2 provider (e.g., Keycloak)
2. Or temporarily disable security for testing (not recommended for production)

### MinIO Not Starting

```bash
# Check MinIO logs
docker-compose logs minio

# Restart MinIO
docker-compose restart minio
```

## Development Tips

1. **Hot Reload**: Use Spring Boot DevTools (if added) or IDE auto-reload
2. **Database Migrations**: Run automatically on startup via Liquibase
3. **Logs**: Check `logs/registry.log` or console output
4. **Profile**: Use `dev` profile for detailed logging: `export SPRING_PROFILES_ACTIVE=dev`
