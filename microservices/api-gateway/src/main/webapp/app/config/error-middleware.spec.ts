/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { applyMiddleware, createStore } from 'redux';

import errorMiddleware from './error-middleware';

describe('Error Middleware', () => {
  let store;

  const reducer = (state = {}, action) => state;

  beforeEach(() => {
    store = createStore(reducer, applyMiddleware(errorMiddleware));
  });

  afterEach(() => {
    (globalThis as any).DEVELOPMENT = false;
    jest.restoreAllMocks();
  });

  it('should not log anything when DEVELOPMENT is false', () => {
    (globalThis as any).DEVELOPMENT = false;
    jest.spyOn(console, 'error').mockImplementation(() => {});

    store.dispatch({ type: 'SOME_ACTION', error: new Error('boom') });

    expect(console.error).not.toHaveBeenCalled();
  });

  it('should not log anything when action has no error', () => {
    (globalThis as any).DEVELOPMENT = true;
    jest.spyOn(console, 'error').mockImplementation(() => {});

    store.dispatch({ type: 'SOME_ACTION' });

    expect(console.error).not.toHaveBeenCalled();
  });

  it('should log the error message when DEVELOPMENT and action has an error without response', () => {
    (globalThis as any).DEVELOPMENT = true;
    jest.spyOn(console, 'error').mockImplementation(() => {});

    store.dispatch({ type: 'SOME_ACTION', error: { message: 'boom' } });

    expect(console.error).toHaveBeenCalledWith(expect.stringContaining('SOME_ACTION caught at middleware'));
    expect(console.error).toHaveBeenCalledTimes(1);
  });

  it('should log the actual cause when error response has data without field errors', () => {
    (globalThis as any).DEVELOPMENT = true;
    jest.spyOn(console, 'error').mockImplementation(() => {});

    store.dispatch({
      type: 'SOME_ACTION',
      error: { message: 'boom', response: { data: { message: 'root cause' } } },
    });

    expect(console.error).toHaveBeenCalledWith(expect.stringContaining('Actual cause: root cause'));
  });

  it('should append field errors to the message when present', () => {
    (globalThis as any).DEVELOPMENT = true;
    jest.spyOn(console, 'error').mockImplementation(() => {});

    store.dispatch({
      type: 'SOME_ACTION',
      error: {
        message: 'boom',
        response: {
          data: {
            message: 'root cause',
            fieldErrors: [{ field: 'name', objectName: 'entity', message: 'must not be null' }],
          },
        },
      },
    });

    expect(console.error).toHaveBeenCalledWith(expect.stringContaining('field: name,  Object: entity, message: must not be null'));
  });
});
