/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- Reports are timed out, never deleted (SW-018), so both tables only grow and every lookup that
-- misses an index gets slower with each report stored.

-- An api_test's results load by api_test alone, which the primary key (api_test_criteria, api_test)
-- does not lead with. PostgreSQL 18's skip scan can still probe it once per criterion, but that is
-- the planner's call - an index of its own makes the lookup a plain one, the way
-- idx_api_test_finding_api_test_result already does for the findings beneath it.
CREATE INDEX idx_api_test_result_api_test
    ON api_test_result (api_test);

-- The report list filters on service name, API name and API version, each optional and independent.
-- uk_api_in_quality_gate_report leads with calculation_id, so it serves none of them. Same shape as
-- api-index-api's api_reference: one index leading with the service, one for each column that can be
-- filtered without it.
CREATE INDEX idx_api_test_service_api_version
    ON api_test (service_name, api_name, api_version);

CREATE INDEX idx_api_test_api_name
    ON api_test (api_name);

CREATE INDEX idx_api_test_api_version
    ON api_test (api_version);
