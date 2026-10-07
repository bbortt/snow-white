/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- The REST layer already rejects anything outside 80-100 (see the OpenAPI spec's
-- minimum/maximum on minCoveragePercentage), but nothing below the DTO validation enforced the
-- same bound — a value written any other way (seeding, a future internal caller) had no backstop.
ALTER TABLE quality_gate_configuration
    ADD CONSTRAINT chk_min_coverage_percentage
        CHECK (min_coverage_percentage BETWEEN 80 AND 100);
