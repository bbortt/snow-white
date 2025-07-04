/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import axios from 'axios';
import sinon from 'sinon';

import setupAxiosInterceptors from './axios-interceptor';

describe('Axios Interceptor', () => {
  describe('setupAxiosInterceptors', () => {
    const client = axios;
    const onUnauthenticated = sinon.spy();
    setupAxiosInterceptors(onUnauthenticated);

    it('onRequestSuccess is called on fulfilled request', () => {
      expect((client.interceptors.request as any).handlers[0].fulfilled({ data: 'foo', url: '/test' })).toMatchObject({
        data: 'foo',
      });
    });
    it('onResponseSuccess is called on fulfilled response', () => {
      expect((client.interceptors.response as any).handlers[0].fulfilled({ data: 'foo' })).toEqual({ data: 'foo' });
    });
    it('onResponseError is called on rejected response', () => {
      const rejectError = {
        response: {
          statusText: 'NotFound',
          status: 401,
          data: { message: 'Page not found' },
        },
      };
      expect((client.interceptors.response as any).handlers[0].rejected(rejectError)).rejects.toEqual(rejectError);
      expect(onUnauthenticated.calledOnce).toBe(true);
    });
  });
});
