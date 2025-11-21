#!/bin/bash

# Run the application with H2 in-memory database
# No Docker or PostgreSQL required!

echo "=========================================="
echo "  Church Registry - H2 Mode"
echo "=========================================="
echo ""
echo "Starting with:"
echo "  - H2 in-memory database"
echo "  - Local filesystem storage"
echo "  - No external dependencies needed"
echo ""
echo "Access points:"
echo "  - Application: http://localhost:8080"
echo "  - H2 Console: http://localhost:8080/h2-console"
echo "  - Health: http://localhost:8080/actuator/health"
echo ""
echo "H2 Console credentials:"
echo "  JDBC URL: jdbc:h2:mem:registry"
echo "  Username: sa"
echo "  Password: (empty)"
echo ""
echo "=========================================="
echo ""

export SPRING_PROFILES_ACTIVE=h2,dev
export STORAGE_TYPE=local
# Don't set JWT_ISSUER_URI for H2 mode - use mock decoder instead
unset JWT_ISSUER_URI
export JWT_AUDIENCE=registry-api

mvn spring-boot:run

