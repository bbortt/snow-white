/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

-- Pin the quality gate's coverage threshold onto the report at creation time.
-- The JUnit export renders a report long after its calculation ran, so it cannot read the
-- threshold from a live gate without letting an edit to that gate retroactively change an already
-- published document.
ALTER TABLE quality_gate_report
    ADD COLUMN min_coverage_percentage INTEGER NOT NULL DEFAULT 100;

-- Reports that predate this column were exported under an unconditional full-coverage bar, which
-- is what a threshold of 100 reproduces exactly. The default backfills them and is then dropped,
-- so every report written from here on must state the threshold it was calculated under.
ALTER TABLE quality_gate_report
    ALTER COLUMN min_coverage_percentage DROP DEFAULT;
