/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- The waiver document a calculation was told, beside the lookback window on report_parameter and
-- the attribute_filters rows hanging off it. A waiver is an input, never state the system keeps:
-- these rows exist only because a request carried them, and nothing writes them afterwards.
--
-- Recorded as asked for rather than as applied. The findings show which waivers took effect; what a
-- review needs six months on is which ones were requested and why, which is the reason column.
CREATE TABLE waiver
(
    calculation_id UUID          NOT NULL,
    -- @OrderColumn: the request's own order, so a re-read answers the document as it was written.
    waiver_index   INTEGER       NOT NULL,
    service_name   VARCHAR(64)   NOT NULL,
    api_name       VARCHAR(64)   NOT NULL,
    -- '*' is stored verbatim where the request wildcarded the version, so the row stays
    -- the document that was submitted rather than the versions it happened to reach.
    api_version    VARCHAR(16)   NOT NULL,
    path           VARCHAR(256)  NOT NULL,
    method         VARCHAR(16)   NOT NULL,
    -- At most one of the three narrow discriminators is set, which the trigger rejects on rather
    -- than the schema: a CHECK here would answer a 500 where the API answers a 400 naming the entry.
    response_code  VARCHAR(16),
    parameter_name VARCHAR(256),
    content_type   VARCHAR(128),
    -- Null where the waiver named no criterion, which waives the target for every criterion that
    -- judged it.
    criterion      VARCHAR(64),
    reason         VARCHAR(1024) NOT NULL,
    expires        DATE          NOT NULL,
    PRIMARY KEY (calculation_id, waiver_index),
    CONSTRAINT fk_waiver_report_parameter
        FOREIGN KEY (calculation_id)
            REFERENCES report_parameter (calculation_id)
            ON DELETE CASCADE
);
