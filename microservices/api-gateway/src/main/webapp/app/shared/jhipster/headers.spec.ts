/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { getMessageFromHeaders } from './headers';

describe('headers', () => {
  describe('getMessageFromHeaders', () => {
    it('should return an empty message when no relevant headers are present', () => {
      expect(getMessageFromHeaders({ 'content-type': 'application/json' })).toEqual({
        alert: undefined,
        error: undefined,
        param: undefined,
      });
    });

    it('should extract alert, error and param regardless of header key casing', () => {
      const result = getMessageFromHeaders({
        'App-Alert': 'app.alert',
        'app-ERROR': 'app.error',
        'app-Params': 'foo',
      });

      expect(result).toEqual({ alert: 'app.alert', error: 'app.error', param: 'foo' });
    });

    it('should unwrap a single-value array header', () => {
      const result = getMessageFromHeaders({ 'app-alert': ['app.alert'] });

      expect(result.alert).toEqual('app.alert');
    });

    it('should throw when an array header has more than one value', () => {
      expect(() => getMessageFromHeaders({ 'app-alert': ['a', 'b'] })).toThrow('Multiple header values found');
    });

    it('should throw when a header value is not a string', () => {
      expect(() => getMessageFromHeaders({ 'app-alert': 42 })).toThrow('Header value is not a string');
    });

    it('should decode url-encoded params and replace + with spaces', () => {
      const result = getMessageFromHeaders({ 'app-params': 'foo+bar%2Fbaz' });

      expect(result.param).toEqual('foo bar/baz');
    });
  });
});
