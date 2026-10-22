/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

// Default JSON paths mirror the api-sync-job's ArtifactoryProperties defaults.
// The api-sync-job uses a parsed OpenAPI object model where extension fields are normalized into an 'extensions' map (e.g. info.extensions.x-service-name).
// The CLI reads raw YAML, so extensions sit directly on their parent object (e.g. info.x-service-name).
export const DEFAULT_API_NAME_PATH = 'info.title';
export const DEFAULT_API_VERSION_PATH = 'info.version';
export const DEFAULT_SERVICE_NAME_PATH = 'info.x-service-name';

export const getNestedValue = (obj: unknown, path: string): string | undefined => {
  const parts = path.split('.');
  let current: unknown = obj;
  for (const part of parts) {
    if (current && typeof current === 'object') {
      current = (current as Record<string, unknown>)[part];
    } else {
      return undefined;
    }
  }
  return typeof current === 'string' ? current : undefined;
};

export interface ApiSpecPaths {
  apiNamePath: string;
  apiVersionPath: string;
  serviceNamePath: string;
}

export interface ApiSpecMetadata {
  apiName: string;
  apiVersion: string;
  serviceName: string;
}

export type ApiSpecResult = { ok: false; missing: string[] } | { ok: true; metadata: ApiSpecMetadata };

/**
 * Extracts API spec metadata from a parsed YAML/JSON document.
 * Returns the metadata on success, or the list of missing field paths on failure.
 */
export const extractApiSpecMetadata = (parsed: unknown, paths: ApiSpecPaths): ApiSpecResult => {
  const apiName = getNestedValue(parsed, paths.apiNamePath);
  const apiVersion = getNestedValue(parsed, paths.apiVersionPath);
  const serviceName = getNestedValue(parsed, paths.serviceNamePath);

  if (apiName && apiVersion && serviceName) {
    return { metadata: { apiName, apiVersion, serviceName }, ok: true };
  }

  const missing: string[] = [];
  if (!apiName) {
    missing.push(paths.apiNamePath);
  }
  if (!apiVersion) {
    missing.push(paths.apiVersionPath);
  }
  if (!serviceName) {
    missing.push(paths.serviceNamePath);
  }
  return { missing, ok: false };
};
