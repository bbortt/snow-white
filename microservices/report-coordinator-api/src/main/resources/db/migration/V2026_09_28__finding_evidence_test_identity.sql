/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- test_case_name stops being a passenger of the trace it arrived on (SW-032).
--
-- V2026_09_24 shipped the column nullable and unused, under a constraint that read the trace as the
-- whole identity. Now that the coverage stream fills it, the pair is the identity (ARCH-011): the
-- same trace naming two tests against one target is two rows, which a suite reusing a trace context
-- across cases reaches, as does a span that named no test beside one that did.

-- The upstream convention bounds the value not at all, and SW-032 forbids truncating it, so the
-- column is widened to hold any real test name. The coverage stream drops a name past 1024 UTF-8
-- bytes rather than truncate it, which is what keeps both this column and the constraint's btree
-- row limit out of reach.
ALTER TABLE finding_evidence
    ALTER COLUMN test_case_name TYPE VARCHAR(1024);

ALTER TABLE finding_evidence
    DROP CONSTRAINT uk_finding_evidence_trace_per_finding;

-- NULLS NOT DISTINCT keeps the duplicate backstop covering the unnamed case, which default NULL
-- semantics would exempt from it. It needs PostgreSQL 15 or newer - the minimum this service
-- documents, and far below the 18.6 every image in this repository is pinned to.
ALTER TABLE finding_evidence
    ADD CONSTRAINT uk_finding_evidence_test_per_trace_per_finding
        UNIQUE NULLS NOT DISTINCT (api_test_finding, trace_id, test_case_name);

-- The new constraint's backing index still leads with api_test_finding, so dropping the old one
-- leaves no gap: finding_evidence needs no separate index on that column alone.
