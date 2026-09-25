/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- api_version was declared wider than every place that actually constrains it — the
-- ApiReference entity (@Size(max = 16)) and the OpenAPI spec (maxLength: 16) already agree on 16;
-- only the column itself allowed 4x that.
ALTER TABLE api_reference
    ALTER COLUMN api_version TYPE VARCHAR(16);
