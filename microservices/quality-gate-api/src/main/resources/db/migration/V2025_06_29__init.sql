/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

CREATE TABLE open_api_coverage_configuration
(
    id   BIGSERIAL   NOT NULL PRIMARY KEY,
    name VARCHAR(32) NOT NULL,
    CONSTRAINT uk_open_api_coverage UNIQUE (name)
);

CREATE TABLE quality_gate_configuration
(
    id            BIGSERIAL   NOT NULL PRIMARY KEY,
    name          VARCHAR(64) NOT NULL,
    description   VARCHAR(256),
    is_predefined BOOLEAN     NOT NULL DEFAULT false,
    CONSTRAINT uk_quality_gate_configuration_name UNIQUE (name)
);

CREATE TABLE quality_gate_open_api_coverage_mapping
(
    open_api_coverage_configuration BIGINT NOT NULL,
    quality_gate_configuration      BIGINT NOT NULL,
    PRIMARY KEY (open_api_coverage_configuration, quality_gate_configuration),
    CONSTRAINT fk_service
        FOREIGN KEY (open_api_coverage_configuration)
            REFERENCES open_api_coverage_configuration (id),
    CONSTRAINT fk_quality_gate
        FOREIGN KEY (quality_gate_configuration)
            REFERENCES quality_gate_configuration (id)
);

CREATE INDEX idx_quality_gate_configuration
    ON quality_gate_configuration (name);
