/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- Mirrors the bound already enforced on the entity (@Min(80) @Max(100)) and on the
-- quality-gate-api source of truth this column pins a copy of — nothing below either enforced it
-- at the database itself.
ALTER TABLE quality_gate_report
    ADD CONSTRAINT chk_min_coverage_percentage
        CHECK (min_coverage_percentage BETWEEN 80 AND 100);
