package com.example.registry.config

import org.hibernate.dialect.H2Dialect

/**
 * Custom H2 dialect for tests that handles TIMESTAMP(0) column definitions.
 * Maps TIMESTAMP(0) to TIMESTAMP since H2 doesn't support the precision syntax.
 * 
 * Note: This dialect alone cannot fix columnDefinition issues because
 * columnDefinition overrides the dialect's type mapping. The actual fix
 * is done via a custom DataSource proxy (H2DataSourceProxy) that intercepts
 * SQL at the JDBC level and modifies DDL statements.
 */
class TestH2Dialect : H2Dialect() {
    // The dialect is used to ensure H2-specific optimizations
    // Actual TIMESTAMP(0) -> TIMESTAMP conversion is done via DataSource proxy
}
