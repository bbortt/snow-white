/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import chalk from 'chalk';
import { exit } from 'node:process';

import type { Waiver } from './sanitized-options';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';

const REQUIRED_FIELDS = Object.freeze(['serviceName', 'apiName', 'apiVersion', 'path', 'method', 'reason'] as const);
const DISCRIMINATORS = Object.freeze(['responseCode', 'parameterName', 'contentType'] as const);
const KNOWN_FIELDS: readonly string[] = Object.freeze([...REQUIRED_FIELDS, ...DISCRIMINATORS, 'criterion', 'expires']);

const ISO_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

const isNonBlankString = (value: unknown): value is string => typeof value === 'string' && value.trim().length > 0;

/**
 * A YAML config file hands `expires: 2026-12-31` over as a `Date` already, a JSON one as a string.
 * Both are accepted, as long as they name a calendar day that exists - `2026-02-30` does not survive
 * the round trip through `Date` and is rejected rather than silently rolled over into March.
 */
const parseExpires = (value: unknown): Date | undefined => {
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? undefined : value;
  }

  if (typeof value !== 'string' || !ISO_DATE_PATTERN.test(value)) {
    return undefined;
  }

  const date = new Date(value);
  return !Number.isNaN(date.getTime()) && date.toISOString().startsWith(value) ? date : undefined;
};

const describeViolations = (entry: Record<string, unknown>): string[] => {
  const violations: string[] = REQUIRED_FIELDS.filter(field => !isNonBlankString(entry[field])).map(
    field => `'${field}' must be a non-blank string`,
  );

  if (!parseExpires(entry.expires)) {
    violations.push("'expires' must be a date in the form YYYY-MM-DD");
  }

  if (DISCRIMINATORS.filter(field => entry[field] !== undefined).length > 1) {
    const discriminators = DISCRIMINATORS.map(field => `'${field}'`).join(', ');
    violations.push(`at most one of ${discriminators} may be set`);
  }

  // A misspelt discriminator would otherwise be dropped on the way out, silently widening the waiver
  // from one response code, parameter or content type to the whole operation.
  Object.keys(entry)
    .filter(field => !KNOWN_FIELDS.includes(field))
    .forEach(field => violations.push(`'${field}' is not a waiver field`));

  return violations;
};

/**
 * Rejects a waiver the server would reject anyway, before the round trip: every waiver needs its
 * target, a `reason` and an `expires`. Whatever needs the calculation's own context to judge - an API
 * not among those calculated, a wildcard outside `apiVersion`, an unknown criterion, an expiry
 * already passed - is left to the server, which is the only place that can judge it.
 */
export const sanitizeWaivers = (waivers: unknown): Waiver[] | undefined => {
  if (waivers === undefined) {
    return undefined;
  }

  if (!Array.isArray(waivers)) {
    console.error(chalk.red('❌ waivers must be a list in the configuration file.'));
    exit(INVALID_CONFIG_FORMAT);
  }

  const invalidWaivers = waivers
    .map((entry: unknown, index) => ({
      number: index + 1,
      violations:
        entry instanceof Object && !Array.isArray(entry) ? describeViolations(entry as Record<string, unknown>) : ['must be an object'],
    }))
    .filter(({ violations }) => violations.length > 0);

  invalidWaivers.forEach(({ number, violations }) => {
    console.error(chalk.red(`❌ Waiver #${number} is invalid:`));
    violations.forEach(violation => console.error(chalk.red(`\t  ${violation}`)));
  });

  if (invalidWaivers.length > 0) {
    exit(INVALID_CONFIG_FORMAT);
  }

  return (waivers as Record<string, unknown>[]).map(entry => ({ ...entry, expires: parseExpires(entry.expires) }) as Waiver);
};
