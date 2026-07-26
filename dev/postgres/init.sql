/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- Users for api-index-api
CREATE USER api_index_flyway WITH PASSWORD 'strongpassword5';
CREATE USER api_index_app WITH PASSWORD 'strongpassword6';

-- Users for report-coordinator-api
CREATE USER report_coord_flyway WITH PASSWORD 'strongpassword1';
CREATE USER report_coord_app WITH PASSWORD 'strongpassword2';

-- Users for quality-gate-api
CREATE USER quality_gate_flyway WITH PASSWORD 'strongpassword3';
CREATE USER quality_gate_app WITH PASSWORD 'strongpassword4';

-- Create the databases
CREATE DATABASE "api-index-api" OWNER api_index_flyway;
CREATE DATABASE "report-coordinator-api" OWNER report_coord_flyway;
CREATE DATABASE "quality-gate-api" OWNER quality_gate_flyway;

-- Now connect to each database to grant privileges
\c "api-index-api"

-- grant privileges for runtime user
GRANT CONNECT ON DATABASE "api-index-api" TO api_index_app;
GRANT USAGE ON SCHEMA public TO api_index_app;

-- connect to the second database
\c "report-coordinator-api"

-- grant privileges for runtime user
GRANT CONNECT ON DATABASE "report-coordinator-api" TO report_coord_app;
GRANT USAGE ON SCHEMA public TO report_coord_app;

-- connect to the third database
\c "quality-gate-api"

-- grant privileges for runtime user
GRANT CONNECT ON DATABASE "quality-gate-api" TO quality_gate_app;
GRANT USAGE ON SCHEMA public TO quality_gate_app;
