/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { applyMiddleware, createStore } from 'redux';

import loggerMiddleware from './logger-middleware';

describe('Logger Middleware', () => {
  let store;

  const reducer = (state = {}, action) => state;

  beforeEach(() => {
    store = createStore(reducer, applyMiddleware(loggerMiddleware));

    jest.spyOn(console, 'groupCollapsed').mockImplementation(() => {});
    jest.spyOn(console, 'groupEnd').mockImplementation(() => {});
    jest.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    (globalThis as any).DEVELOPMENT = false;
    jest.restoreAllMocks();
  });

  it('should not log anything when DEVELOPMENT is false', () => {
    (globalThis as any).DEVELOPMENT = false;

    store.dispatch({ type: 'SOME_ACTION', payload: 'foo' });

    expect(console.groupCollapsed).not.toHaveBeenCalled();
  });

  it('should log payload and meta when DEVELOPMENT and action has no error', () => {
    (globalThis as any).DEVELOPMENT = true;

    store.dispatch({ type: 'SOME_ACTION', payload: 'foo', meta: { bar: 'baz' } });

    expect(console.groupCollapsed).toHaveBeenCalledWith('SOME_ACTION');
    expect(console.log).toHaveBeenCalledWith('Payload:', 'foo');
    expect(console.log).not.toHaveBeenCalledWith('Error:', expect.anything());
    expect(console.log).toHaveBeenCalledWith('Meta:', { bar: 'baz' });
    expect(console.groupEnd).toHaveBeenCalled();
  });

  it('should log the error when present', () => {
    (globalThis as any).DEVELOPMENT = true;
    const error = new Error('boom');

    store.dispatch({ type: 'SOME_ACTION', error });

    expect(console.log).toHaveBeenCalledWith('Error:', error);
  });
});
