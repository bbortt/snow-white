/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- idx_quality_gate_configuration covered the same single column as uk_quality_gate_configuration_name
-- — that unique constraint's own backing index already serves any lookup on name, so this one added
-- nothing but write overhead.
DROP INDEX idx_quality_gate_configuration;
