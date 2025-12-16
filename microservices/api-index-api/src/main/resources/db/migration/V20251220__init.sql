/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

CREATE TABLE api_reference
(
    otel_service_name VARCHAR(64) NOT NULL,
    api_name          VARCHAR(64) NOT NULL,
    api_version       VARCHAR(64) NOT NULL,
    source_url        VARCHAR(64) NOT NULL,
    api_type          VARCHAR(16) NOT NULL,
    PRIMARY KEY (otel_service_name, api_name, api_version)
);
