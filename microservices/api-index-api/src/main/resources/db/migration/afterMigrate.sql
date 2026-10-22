/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ${app_schema} TO ${app_user};
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA ${app_schema} TO ${app_user};
