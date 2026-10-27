/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import java.util.List;

/**
 * The allowlisted v1 tool set, written out. Deliberately a test-side literal rather than a
 * read of whatever the production code registers from: a test comparing a value to itself
 * passes however that value changes, and a mutation applied to one shared constant would
 * change both sides identically. Adding a tool fails the transport tests until someone
 * edits this list, which is where the read-only question gets asked.
 */
public final class ReadOnlyToolSetV1 {

  public static final List<String> NAMES = List.of(
    "explain_criterion",
    "get_coverage",
    "list_apis",
    "list_findings"
  );

  private ReadOnlyToolSetV1() {}
}
