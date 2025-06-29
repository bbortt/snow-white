/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

CREATE TABLE open_api_coverage_configuration
(
    service_name VARCHAR PRIMARY KEY,
    enable       BOOLEAN NOT NULL
);

CREATE TABLE quality_gate_configuration
(
    service_name        VARCHAR PRIMARY KEY,
    enable              BOOLEAN NOT NULL,
    block_pull_requests BOOLEAN NOT NULL
);

CREATE TABLE quality_gate_open_api_coverage_mapping
(
    id                BIGSERIAL PRIMARY KEY,
    service_name      VARCHAR NOT NULL,
    quality_gate_name VARCHAR NOT NULL,
    CONSTRAINT fk_service
        FOREIGN KEY (service_name)
            REFERENCES open_api_coverage_configuration (service_name)
            ON DELETE CASCADE,
    CONSTRAINT fk_quality_gate
        FOREIGN KEY (quality_gate_name)
            REFERENCES quality_gate_configuration (service_name)
            ON DELETE CASCADE
);

CREATE INDEX idx_qgocm_service_name
    ON quality_gate_open_api_coverage_mapping (service_name);

CREATE INDEX idx_qgocm_quality_gate_name
    ON quality_gate_open_api_coverage_mapping (quality_gate_name);
