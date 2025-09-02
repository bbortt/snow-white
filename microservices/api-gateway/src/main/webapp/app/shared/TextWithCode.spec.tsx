/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import React from 'react';

import { TextWithCode } from './TextWithCode';

describe('TextWithCode', () => {
  it.each(['', undefined])('should handle empty string gracefully', (emptyString: string | undefined) => {
    render(<TextWithCode text={emptyString} />);
    expect(screen.getAllByText('')).toHaveLength(3);
    expect(screen.queryByRole('code')).not.toBeInTheDocument();
  });

  it('should render plain text when no backticks are present', () => {
    render(<TextWithCode text="Hello world" />);
    expect(screen.getByText('Hello world')).toBeInTheDocument();
    expect(screen.queryByRole('code')).not.toBeInTheDocument();
  });

  it('should render text as plain span when backticks are unbalanced', () => {
    render(<TextWithCode text="This is `broken" />);
    expect(screen.getByText('This is `broken')).toBeInTheDocument();
    expect(screen.queryByRole('code')).not.toBeInTheDocument();
  });

  it('should render code inside backticks as <code>', () => {
    const input = 'Every path defined in the OpenAPI specification has been called. This is a subset of `HTTP_METHOD_COVERAGE`.';
    render(<TextWithCode text={input} />);

    expect(screen.getByText('Every path defined in the OpenAPI specification has been called. This is a subset of')).toBeInTheDocument();
    expect(screen.getByText('HTTP_METHOD_COVERAGE').tagName).toBe('CODE');
    expect(screen.getByText('.')).toBeInTheDocument();
  });

  it('should render multiple code segments correctly', () => {
    const input = 'Use `npm install` and then `npm test` to verify.';
    render(<TextWithCode text={input} />);

    const codes = screen.getAllByText(/npm/);
    expect(codes.length).toBe(2);
    expect(codes[0].tagName).toBe('CODE');
    expect(codes[1].tagName).toBe('CODE');
  });
});
