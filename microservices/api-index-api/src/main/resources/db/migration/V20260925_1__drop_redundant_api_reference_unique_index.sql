/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- uq_api_reference_service_api_version covered the exact same columns, in the exact same order,
-- as the table's own primary key — the PK's backing index already served every lookup this one
-- could, so it was pure write overhead with no query it uniquely enabled.
DROP INDEX uq_api_reference_service_api_version;
