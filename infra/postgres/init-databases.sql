-- Runs once on an empty PostgreSQL data volume. The initialization user owns both DBs.
-- Flyway in each backend service creates its tables when the service starts.
-- POSTGRES_DB creates shopee_user; this script creates the second service database.
CREATE DATABASE shopee_product;
