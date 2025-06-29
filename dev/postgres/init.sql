/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- Users for report-coordination-service
CREATE USER report_coord_flyway WITH PASSWORD 'strongpassword1';
CREATE USER report_coord_app WITH PASSWORD 'strongpassword2';

-- Users for quality-gate-api
CREATE USER quality_gate_flyway WITH PASSWORD 'strongpassword3';
CREATE USER quality_gate_app WITH PASSWORD 'strongpassword4';

-- Create the databases
CREATE DATABASE "report-coordination-service" OWNER report_coord_flyway;
CREATE DATABASE "quality-gate-api" OWNER quality_gate_flyway;

-- Now connect to each database to grant privileges
\c "report-coordination-service"

-- grant privileges for runtime user
GRANT CONNECT ON DATABASE "report-coordination-service" TO report_coord_app;
GRANT USAGE ON SCHEMA public TO report_coord_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO report_coord_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO report_coord_app;

-- allow future tables to be accessible by app user
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO report_coord_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO report_coord_app;

-- connect to the second database
\c "quality-gate-api"

-- grant privileges for runtime user
GRANT CONNECT ON DATABASE "quality-gate-api" TO quality_gate_app;
GRANT USAGE ON SCHEMA public TO quality_gate_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO quality_gate_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO quality_gate_app;

-- allow future tables to be accessible by app user
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO quality_gate_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO quality_gate_app;
