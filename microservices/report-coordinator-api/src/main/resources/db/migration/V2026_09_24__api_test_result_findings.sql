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

-- One row per (trace_id, test_case_name) pair a finding was matched by. test_case_name is the test
-- identity the evidencing span carried (SW-032) and stays null where it carried none, which is every
-- span until a consumer's test harness emits the convention.
-- Its length is a storage bound Snow-White chooses: the upstream convention bounds the value not at
-- all, and SW-032 forbids truncating it, so the column is wide enough for any real test name. The
-- coverage stream drops a name past 1024 UTF-8 bytes rather than truncate it, which is what keeps
-- both this column and the constraint's btree row limit out of reach.
CREATE TABLE finding_evidence
(
    api_test_finding BIGINT      NOT NULL,
    trace_id         VARCHAR(64) NOT NULL,
    test_case_name   VARCHAR(1024),
    CONSTRAINT fk_api_test_finding_finding_evidence
        FOREIGN KEY (api_test_finding)
            REFERENCES api_test_finding (id)
            ON DELETE CASCADE,
    -- The pair is the identity (ARCH-011), so the same trace naming two tests against one target is
    -- two rows: a suite reusing a trace context across cases, or a span that named no test beside
    -- one that did, are both reachable. NULLS NOT DISTINCT keeps the duplicate backstop covering the
    -- unnamed case, which default NULL semantics would exempt from it. NULLS NOT DISTINCT needs
    -- PostgreSQL 15 or newer; every image this repository runs is pinned to 18.6.
    CONSTRAINT uk_finding_evidence_test_per_trace_per_finding
        UNIQUE NULLS NOT DISTINCT (api_test_finding, trace_id, test_case_name)
);

CREATE INDEX idx_api_test_finding_api_test_result
    ON api_test_finding (api_test_criteria, api_test);

-- No separate index on finding_evidence(api_test_finding) alone: the unique constraint's backing
-- index already leads with that column.

-- "Which targets did this trace cover?" is the drilldown's reverse question, and the only one that
-- reaches this table without a finding in hand.
CREATE INDEX idx_finding_evidence_trace_id
    ON finding_evidence (trace_id);
