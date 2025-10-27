/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

CREATE TABLE quality_gate_report
(
    calculation_id           UUID        NOT NULL PRIMARY KEY,
    quality_gate_config_name VARCHAR(64) NOT NULL,
    open_api_coverage_status VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    report_status            VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE report_parameter
(
    calculation_id  UUID       NOT NULL PRIMARY KEY,
    lookback_window VARCHAR(8) NOT NULL DEFAULT '1h',
    CONSTRAINT fk_quality_gate_report_report_parameter
        FOREIGN KEY (calculation_id)
            REFERENCES quality_gate_report (calculation_id)
            ON DELETE CASCADE
);

CREATE TABLE attribute_filters
(
    calculation_id  UUID        NOT NULL,
    attribute_key   VARCHAR(64) NOT NULL,
    attribute_value VARCHAR(64),
    PRIMARY KEY (calculation_id, attribute_key),
    CONSTRAINT fk_attribute_filters_report_parameter
        FOREIGN KEY (calculation_id)
            REFERENCES report_parameter (calculation_id)
            ON DELETE CASCADE
);

CREATE TABLE api_test
(
    id             BIGSERIAL PRIMARY KEY NOT NULL,
    service_name   VARCHAR(256)          NOT NULL,
    api_name       VARCHAR(256)          NOT NULL,
    api_version    VARCHAR(16),
    api_type       SMALLINT,
    calculation_id UUID                  NOT NULL,
    CONSTRAINT fk_quality_gate_report_api_test
        FOREIGN KEY (calculation_id)
            REFERENCES quality_gate_report (calculation_id)
            ON DELETE CASCADE,
    CONSTRAINT uk_api_in_quality_gate_report
        UNIQUE (calculation_id, service_name, api_name, api_version, api_type)
);

CREATE TABLE api_test_result
(
    api_test_criteria      VARCHAR(32)   NOT NULL,
    coverage               DECIMAL(3, 2) NOT NULL,
    included_in_report     BOOLEAN       NOT NULL,
    duration               BIGINT        NOT NULL, -- Duration stored as nanoseconds
    additional_information VARCHAR(256),
    api_test               BIGINT        NOT NULL,
    PRIMARY KEY (api_test_criteria, api_test),
    CONSTRAINT fk_api_test_api_test_result
        FOREIGN KEY (api_test)
            REFERENCES api_test (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_quality_gate_report_status
    ON quality_gate_report (created_at);

CREATE INDEX idx_quality_gate_report_config_name
    ON quality_gate_report (quality_gate_config_name);
