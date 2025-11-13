# Running with H2 Database (No Docker/PostgreSQL)

This guide shows you how to run the application using H2 in-memory database - perfect for local development without any external dependencies.

## Quick Start

```bash
# Option 1: Use the provided script
./run-h2.sh

# Option 2: Manual command
export SPRING_PROFILES_ACTIVE=h2,dev
export STORAGE_TYPE=local
mvn spring-boot:run
```

That's it! No Docker, no PostgreSQL setup needed.

## What Gets Configured

When using the `h2` profile:

- **Database**: H2 in-memory (data is lost on restart)
- **Storage**: Local filesystem (`./storage` directory)
- **Schema**: Auto-created by Hibernate from entities
- **Seed Data**: Automatically loaded from `data-h2.sql`
- **H2 Console**: Available at http://localhost:8080/h2-console

## Access Points

Once running:

- **Application**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:registry`
  - Username: `sa`
  - Password: (leave empty)
- **Health Check**: http://localhost:8080/actuator/health
- **Public Homepage**: http://localhost:8080/public/sample-parish/home

## Important Notes

1. **Data Persistence**: H2 in-memory database loses all data when the application stops
2. **JWT/OAuth2**: You'll need to provide valid JWT tokens for authenticated endpoints, or configure a mock OAuth2 provider
3. **PostgreSQL Features**: Some PostgreSQL-specific features may not work perfectly with H2, but core functionality should work
4. **Testing**: This setup is ideal for development and testing, but use PostgreSQL for production

## Troubleshooting

### Application Won't Start

Check that:
- Java 21+ is installed: `java -version`
- Maven is available: `mvn --version`
- Port 8080 is not in use

### H2 Console Not Accessible

- Make sure you're using the `h2` profile
- Check that H2 console is enabled in `application-h2.yml`
- Access at: http://localhost:8080/h2-console

### Schema Creation Issues

If you see errors about table creation:
- Check logs for specific SQL errors
- H2 may not support all PostgreSQL features
- Some entities may need adjustment for H2 compatibility

## Next Steps

Once running, you can:
1. Test the public endpoints (no auth required)
2. Set up OAuth2/JWT for authenticated endpoints
3. Switch to PostgreSQL when ready for production

