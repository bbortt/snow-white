/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { afterEach, beforeEach, describe, expect, it, mock, spyOn } from 'bun:test';
import { exit } from 'node:process';

import { INVALID_CONFIG_FORMAT } from '../common/exit-codes';
import { sanitizeWaivers } from './sanitize-waivers';

await mock.module('node:process', () => ({
  exit: mock().mockImplementation((code: number) => {
    throw new Error(`Process exited with code ${code}`);
  }),
}));

const waiver = {
  apiName: 'orders-api',
  apiVersion: '1.0.0',
  expires: '2026-12-31',
  method: 'POST',
  path: '/orders/{id}',
  reason: 'Downstream is mocked in this environment',
  responseCode: '500',
  serviceName: 'order-service',
};

describe('sanitizeWaivers', () => {
  let consoleErrorSpy: ReturnType<typeof spyOn>;

  beforeEach(() => {
    consoleErrorSpy = spyOn(console, 'error').mockImplementation(() => {});

    // @ts-expect-error TS2339: Property mockClear does not exist on type exit
    exit.mockClear();
  });

  afterEach(() => {
    consoleErrorSpy.mockRestore();
  });

  const expectRejected = (waivers: unknown[], ...violations: string[]) => {
    expect(() => sanitizeWaivers(waivers)).toThrowError(`Process exited with code ${INVALID_CONFIG_FORMAT}`);

    expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Waiver #1 is invalid:'));
    violations.forEach(violation => expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining(violation)));
    expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
  };

  describe('valid waivers', () => {
    it('should return undefined when there are no waivers', () => {
      expect(sanitizeWaivers(undefined)).toBeUndefined();

      expect(exit).not.toHaveBeenCalled();
    });

    it('should return an empty list as an empty list', () => {
      expect(sanitizeWaivers([])).toEqual([]);
    });

    it('should parse a JSON expires string into a UTC date of that day', () => {
      const [sanitized] = sanitizeWaivers([waiver])!;

      expect(sanitized).toEqual({ ...waiver, expires: new Date('2026-12-31') });
      expect(sanitized!.expires.toISOString()).toBe('2026-12-31T00:00:00.000Z');
    });

    it('should keep an expires a YAML config file already parsed into a date', () => {
      const expires = new Date('2026-12-31');

      expect(sanitizeWaivers([{ ...waiver, expires }])).toEqual([{ ...waiver, expires }]);
    });

    it('should accept a leap day that exists', () => {
      expect(sanitizeWaivers([{ ...waiver, expires: '2028-02-29' }])![0]!.expires).toEqual(new Date('2028-02-29'));
    });

    it('should accept a waiver addressing the whole operation, with no discriminator', () => {
      const { responseCode: _responseCode, ...operationWaiver } = waiver;

      expect(sanitizeWaivers([operationWaiver])).toEqual([{ ...operationWaiver, expires: new Date('2026-12-31') }]);
    });

    it.each(['parameterName', 'contentType'])('should accept %s as the single discriminator', (discriminator: string) => {
      const { responseCode: _responseCode, ...operationWaiver } = waiver;

      expect(() => sanitizeWaivers([{ ...operationWaiver, [discriminator]: 'value' }])).not.toThrow();
    });

    it('should accept the optional criterion', () => {
      const narrowed = { ...waiver, criterion: 'ERROR_RESPONSE_CODE_COVERAGE' };

      expect(sanitizeWaivers([narrowed])![0]).toHaveProperty('criterion', 'ERROR_RESPONSE_CODE_COVERAGE');
    });

    it('should leave judging an already passed expiry to the server', () => {
      expect(() => sanitizeWaivers([{ ...waiver, expires: '2000-01-01' }])).not.toThrow();
    });

    it("should leave judging a '*' outside apiVersion to the server", () => {
      expect(() => sanitizeWaivers([{ ...waiver, apiName: '*', apiVersion: '*' }])).not.toThrow();
    });

    it('should not modify the waivers it was given', () => {
      const given = [{ ...waiver }];

      sanitizeWaivers(given);

      expect(given).toEqual([waiver]);
    });
  });

  describe('invalid waivers', () => {
    it.each([[{}], ['waivers'], [42]])('should exit with code 3 if waivers is %p rather than a list', (waivers: unknown) => {
      expect(() => sanitizeWaivers(waivers)).toThrowError(`Process exited with code ${INVALID_CONFIG_FORMAT}`);

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ waivers must be a list in the configuration file.'));
      expect(exit).toHaveBeenCalledWith(INVALID_CONFIG_FORMAT);
    });

    it.each(['serviceName', 'apiName', 'apiVersion', 'path', 'method', 'reason'])(
      'should exit with code 3 if a waiver has no %s',
      field => {
        const { [field as keyof typeof waiver]: _omitted, ...incomplete } = waiver;

        expectRejected([incomplete], `'${field}' must be a non-blank string`);
      },
    );

    it.each(['', '   ', 42, null])('should exit with code 3 if a waiver gives %p as reason', (reason: unknown) => {
      expectRejected([{ ...waiver, reason }], "'reason' must be a non-blank string");
    });

    it.each([undefined, null, '31.12.2026', '2026-12-31T00:00:00Z', '2026-02-30', '2026-13-01', 20261231, new Date('not a date')])(
      'should exit with code 3 if a waiver expires on %p',
      (expires: unknown) => {
        expectRejected([{ ...waiver, expires }], "'expires' must be a date in the form YYYY-MM-DD");
      },
    );

    it('should exit with code 3 if a waiver sets more than one discriminator', () => {
      expectRejected(
        [{ ...waiver, contentType: 'application/json', parameterName: 'page' }],
        "at most one of 'responseCode', 'parameterName', 'contentType' may be set",
      );
    });

    it('should exit with code 3 if a waiver carries a field it does not know, rather than widening the waiver', () => {
      const { responseCode, ...rest } = waiver;

      expectRejected([{ ...rest, reponseCode: responseCode }], "'reponseCode' is not a waiver field");
    });

    // wrapped, because `it.each` spreads an array row into arguments
    it.each([[null], ['waiver'], [42], [[waiver]]])('should exit with code 3 if a waiver is %p rather than an object', (entry: unknown) => {
      expectRejected([entry], 'must be an object');
    });

    it('should list every violation of one waiver, not just the first', () => {
      expectRejected(
        [{ ...waiver, contentType: 'application/json', expires: 'soon', reason: '' }],
        "'reason' must be a non-blank string",
        "'expires' must be a date in the form YYYY-MM-DD",
        'at most one of',
      );
    });

    it('should name every invalid waiver before exiting, by its position in the list', () => {
      expect(() => sanitizeWaivers([{ ...waiver, reason: '' }, waiver, { ...waiver, method: '' }])).toThrowError(
        `Process exited with code ${INVALID_CONFIG_FORMAT}`,
      );

      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Waiver #1 is invalid:'));
      expect(consoleErrorSpy).not.toHaveBeenCalledWith(expect.stringContaining('❌ Waiver #2 is invalid:'));
      expect(consoleErrorSpy).toHaveBeenCalledWith(expect.stringContaining('❌ Waiver #3 is invalid:'));
      expect(exit).toHaveBeenCalledTimes(1);
    });
  });
});
