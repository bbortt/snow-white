/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */
/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

CREATE TABLE report_parameters
(
    id              BIGSERIAL    NOT NULL PRIMARY KEY,
    service_name    VARCHAR(256) NOT NULL,
    api_name        VARCHAR(256) NOT NULL,
    api_version     VARCHAR(16),
    lookback_window VARCHAR(8)   NOT NULL DEFAULT '1h'
);

CREATE TABLE quality_gate_report
(
    calculation_id           UUID        NOT NULL PRIMARY KEY,
    quality_gate_config_name VARCHAR(64) NOT NULL,
    report_parameters_id     BIGINT      NOT NULL,
    open_api_coverage_status VARCHAR(16) NOT NULL DEFAULT 'NOT_STARTED',
    report_status            VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quality_gate_report_parameters
        FOREIGN KEY (report_parameters_id)
            REFERENCES report_parameters (id)
            ON DELETE CASCADE
);

CREATE TABLE attribute_filters
(
    report_parameter_id BIGINT      NOT NULL,
    attribute_key       VARCHAR(64) NOT NULL,
    attribute_value     VARCHAR(64),
    PRIMARY KEY (report_parameter_id, attribute_key),
    CONSTRAINT fk_attribute_filters_report_parameters
        FOREIGN KEY (report_parameter_id)
            REFERENCES report_parameters (id)
            ON DELETE CASCADE
);

CREATE TABLE open_api_test_result
(
    open_api_test_criteria VARCHAR(32)   NOT NULL,
    calculation_id         UUID          NOT NULL,
    coverage               DECIMAL(3, 2) NOT NULL,
    included_in_report     BOOLEAN       NOT NULL,
    duration               BIGINT        NOT NULL, -- Duration stored as nanoseconds
    additional_information VARCHAR(256),
    PRIMARY KEY (open_api_test_criteria, calculation_id),
    CONSTRAINT fk_open_api_test_result_quality_gate_report
        FOREIGN KEY (calculation_id)
            REFERENCES quality_gate_report (calculation_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_quality_gate_report_status
    ON quality_gate_report (created_at);

CREATE INDEX idx_quality_gate_report_config_name
    ON quality_gate_report (quality_gate_config_name);
