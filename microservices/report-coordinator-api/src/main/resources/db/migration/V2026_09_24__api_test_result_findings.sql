/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- What a criterion concluded about one target of the specification, beside the ratio those verdicts
-- add up to. api_test_result.coverage stays as the denormalized cache every list-shaped read uses;
-- these rows are the evidence it has to agree with.
CREATE TABLE api_test_finding
(
    id                BIGSERIAL PRIMARY KEY NOT NULL,
    status            SMALLINT              NOT NULL, -- stable code, not an ordinal
    spec_pointer      VARCHAR(512)          NOT NULL,
    http_path         VARCHAR(256),
    http_method       VARCHAR(16),
    response_code     VARCHAR(16),
    parameter_name    VARCHAR(256),
    content_type      VARCHAR(128),
    api_test_criteria VARCHAR(64)           NOT NULL,
    api_test          BIGINT                NOT NULL,
    CONSTRAINT fk_api_test_result_api_test_finding
        FOREIGN KEY (api_test_criteria, api_test)
            REFERENCES api_test_result (api_test_criteria, api_test)
            ON DELETE CASCADE
);

-- One row per (trace_id, test_case_name) pair a finding was matched by. test_case_name ships
-- nullable and stays null until a consumer's test harness emits the convention and the narrowed
-- attribute set requests it: a nullable column on an empty table costs nothing now, and the same
-- change to a published API component later does not.
CREATE TABLE finding_evidence
(
    api_test_finding BIGINT      NOT NULL,
    trace_id         VARCHAR(64) NOT NULL,
    test_case_name   VARCHAR(256),
    CONSTRAINT fk_api_test_finding_finding_evidence
        FOREIGN KEY (api_test_finding)
            REFERENCES api_test_finding (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_api_test_finding_api_test_result
    ON api_test_finding (api_test_criteria, api_test);

CREATE INDEX idx_finding_evidence_api_test_finding
    ON finding_evidence (api_test_finding);

-- "Which targets did this trace cover?" is the drilldown's reverse question, and the only one that
-- reaches this table without a finding in hand.
CREATE INDEX idx_finding_evidence_trace_id
    ON finding_evidence (trace_id);
