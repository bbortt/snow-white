/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export const defaultLogPattern =
  '%d{yyyy-MM-dd\'T\'HH:mm:ss.SSSXXX} level="%level" thread="%thread" logger="%logger"{2}%replace(%mdc{trace_id}){^(.+)$, traceId="$1"} msg="%msg"%n%ex';
