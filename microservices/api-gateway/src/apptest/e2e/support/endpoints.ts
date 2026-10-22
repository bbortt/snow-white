/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

export const managementInfo = (): RegExp => /\/management\/info$/;

export const reports = (): RegExp => /\/api\/rest\/v1\/reports(\?.*)?$/;
export const reportById = (calculationId: string): RegExp => new RegExp(`/api/rest/v1/reports/${escapeRegExp(calculationId)}$`);
export const reportJunit = (calculationId: string): RegExp => new RegExp(`/api/rest/v1/reports/${escapeRegExp(calculationId)}/junit$`);

export const qualityGates = (): RegExp => /\/api\/rest\/v1\/quality-gates(\?.*)?$/;
export const qualityGateByName = (name: string): RegExp =>
  new RegExp(`/api/rest/v1/quality-gates/${escapeRegExp(encodeURIComponent(name))}$`);

export const criteria = (): RegExp => /\/api\/rest\/v1\/criteria\/openapi$/;

export const apis = (): RegExp => /\/api\/rest\/v1\/apis(\?.*)?$/;
export const apiNames = (): RegExp => /\/api\/rest\/v1\/apis\/meta\/api-names(\?.*)?$/;
export const serviceNames = (): RegExp => /\/api\/rest\/v1\/apis\/meta\/service-names(\?.*)?$/;
