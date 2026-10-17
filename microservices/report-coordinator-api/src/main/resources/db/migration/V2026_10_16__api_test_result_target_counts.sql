/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- What the coverage ratio was taken over: the size of the judged set it divided by, and the number
-- of targets a waiver took out of that set before it was measured. Both come out of the one
-- reduction that produced api_test_result.coverage and travel on the result event beside it, so
-- nothing here is recomputed from the findings on a read.
--
-- Nullable, and deliberately without a default. Every result this pipeline writes carries both, but
-- every result written before this migration carries neither, and 0 is not the honest value for
-- those: judged_target_count = 0 asserts that the criterion judged nothing, which is precisely the
-- claim a vacuous coverage of 1.00 must never be allowed to make silently. A null says "this row
-- does not know", and the API omits the key rather than publishing a number nobody computed.
ALTER TABLE api_test_result
    ADD COLUMN judged_target_count INTEGER,
    ADD COLUMN waived_target_count INTEGER;

-- The justification a waived finding was waived under, carried on the finding rather than joined
-- back to the waiver rows: every consumer already walks the findings, and a parallel record would
-- make the report read, the export and the drilldown each agree about a join.
--
-- Null for every finding that is not WAIVED, which is what the status already says. The two columns
-- are set together or not at all - a reason without an expiry describes a waiver this system cannot
-- have applied, since the trigger requires both.
ALTER TABLE api_test_finding
    ADD COLUMN waiver_reason VARCHAR(1024),
    ADD COLUMN waiver_expires_on DATE;
