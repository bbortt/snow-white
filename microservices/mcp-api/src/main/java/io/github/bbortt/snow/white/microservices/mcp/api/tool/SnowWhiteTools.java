/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SysTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSys;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.api.CriteriaApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.api.ReportApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.ListQualityGateReports200ResponseInner;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The read-only tool set an agentic client queries. Every tool is one call through a
 * generated REST client against an endpoint that already exists: nothing here computes,
 * compares, aggregates or re-derives a verdict, and the responses are the clients' own
 * types so no field can be dropped on the way out.
 */
@RealizesSys(SysTraceables.SYS_015_AGENT_QUERYABLE_CONSUMPTION_PATH)
@RealizesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
@RealizesCon(ConTraceables.CON_014_MCP_TOOL_SET_IS_READ_ONLY)
@Component
@RequiredArgsConstructor
public class SnowWhiteTools {

  private final ApiIndexApi apiIndexApi;
  private final CriteriaApi criteriaApi;
  private final ReportApi reportApi;

  @McpTool(
    name = "list_apis",
    description = "List the API specifications Snow-White has indexed, optionally narrowed to one service or one API name. Use this to discover what can be asked about before calling the other tools."
  )
  public List<GetAllApis200ResponseInner> listApis(
    @McpToolParam(
      required = false,
      description = "OpenTelemetry service name owning the API, e.g. 'payments-service'."
    ) @Nullable String serviceName,
    @McpToolParam(
      required = false,
      description = "API name as indexed, e.g. 'payments-api'."
    ) @Nullable String apiName
  ) {
    return apiIndexApi.getAllApis(null, null, null, serviceName, apiName);
  }

  @McpTool(
    name = "explain_criterion",
    description = "Describe one OpenAPI coverage criterion, so a criterion referenced by a report can be reasoned about rather than guessed at from its name."
  )
  public OpenApiCriterion explainCriterion(
    @McpToolParam(
      required = true,
      description = "Identifier of the criterion, as it appears in a quality-gate report."
    ) String criterionId
  ) {
    return criteriaApi
      .listOpenApiCoverageCriteria()
      .stream()
      .filter(criterion -> criterion.getId().equals(criterionId))
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException(
          "No OpenAPI coverage criterion is named '%s'.".formatted(criterionId)
        )
      );
  }

  @McpTool(
    name = "get_coverage",
    description = "Return the most recent quality-gate report covering a service or API, including each tested interface and its per-criterion coverage. Does not state whether the gate passed; read the report's own status for that."
  )
  public ListQualityGateReports200ResponseInner getCoverage(
    @McpToolParam(
      required = false,
      description = "Restrict to reports including an API of this OpenTelemetry service name."
    ) @Nullable String serviceName,
    @McpToolParam(
      required = false,
      description = "Restrict to reports including an API of this name."
    ) @Nullable String apiName,
    @McpToolParam(
      required = false,
      description = "Restrict to reports including this API version."
    ) @Nullable String apiVersion
  ) {
    // The listing already orders by initiatedAt descending, so one page of one is the latest.
    return reportApi
      .listQualityGateReports(null, 1, null, serviceName, apiName, apiVersion)
      .stream()
      .findFirst()
      .orElseThrow(() ->
        new IllegalArgumentException(
          "No quality-gate report matches service '%s', API '%s', version '%s'.".formatted(
            serviceName,
            apiName,
            apiVersion
          )
        )
      );
  }

  @McpTool(
    name = "list_findings",
    description = "Return one quality-gate report in full, including every finding with its JSON Pointer into the specification and the trace evidence behind it. This is what makes a finding actionable rather than merely true."
  )
  public GetReportByCalculationId200Response listFindings(
    @McpToolParam(
      required = true,
      description = "UUID of the calculation, as returned by get_coverage."
    ) UUID calculationId
  ) {
    return reportApi.getReportByCalculationId(calculationId);
  }
}
