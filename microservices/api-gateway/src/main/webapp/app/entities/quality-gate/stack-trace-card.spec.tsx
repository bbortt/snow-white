/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';

import { StackTraceCard } from './stack-trace-card';

jest.mock('@fortawesome/react-fontawesome', () => ({
  FontAwesomeIcon: ({ icon }: { icon: string }) => <span data-testid="icon">{icon}</span>,
}));

jest.mock('app/entities/quality-gate/code-highlight-block', () => ({
  CodeHighlightBlock: ({ code }: { code: string }) => <pre data-testid="code-highlight-block">{code}</pre>,
}));

jest.mock('react-jhipster', () => ({
  Translate: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

jest.mock('reactstrap', () => ({
  Collapse: ({ isOpen, children }: { isOpen: boolean; children: React.ReactNode }) => (
    <div data-testid="collapse" data-open={String(isOpen)}>
      {isOpen ? children : null}
    </div>
  ),
}));

jest.mock('./stack-trace-card.scss', () => ({}));

const STACK_TRACE = 'java.lang.NullPointerException\n\tat com.example.Foo.bar(Foo.java:42)';

describe('StackTraceCard', () => {
  it('renders the "Stack Trace" label', () => {
    render(<StackTraceCard stackTrace={STACK_TRACE} />);

    expect(screen.getByText('Stack Trace')).toBeInTheDocument();
  });

  it('renders the toggle as a <dt> with the correct class', () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    const dt = container.querySelector('dt');
    expect(dt).toBeInTheDocument();
    expect(dt).toHaveClass('stack-trace-card__toggle');
  });

  it('is collapsed by default', () => {
    render(<StackTraceCard stackTrace={STACK_TRACE} />);

    expect(screen.getByTestId('collapse')).toHaveAttribute('data-open', 'false');
    expect(screen.queryByTestId('code-highlight-block')).not.toBeInTheDocument();
  });

  it('shows the chevron-down icon when collapsed', () => {
    render(<StackTraceCard stackTrace={STACK_TRACE} />);

    expect(screen.getByTestId('icon')).toHaveTextContent('chevron-down');
  });

  it('expands and renders CodeHighlightBlock when the button is clicked', async () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    await userEvent.click(container.querySelector('button')!);

    expect(screen.getByTestId('collapse')).toHaveAttribute('data-open', 'true');
    expect(screen.getByTestId('code-highlight-block')).toBeInTheDocument();
  });

  it('passes the stackTrace to CodeHighlightBlock', async () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    await userEvent.click(container.querySelector('button')!);

    expect(screen.getByTestId('code-highlight-block')).toHaveTextContent(
      'java.lang.NullPointerException at com.example.Foo.bar(Foo.java:42)',
    );
  });

  it('switches to chevron-up icon when expanded', async () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    await userEvent.click(container.querySelector('button')!);

    expect(screen.getByTestId('icon')).toHaveTextContent('chevron-up');
  });

  it('collapses again on a second click', async () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);
    const button = container.querySelector('button')!;

    await userEvent.click(button);
    await userEvent.click(button);

    expect(screen.getByTestId('collapse')).toHaveAttribute('data-open', 'false');
    expect(screen.queryByTestId('code-highlight-block')).not.toBeInTheDocument();
  });

  it('adds mb-4 class to <dd> when expanded', async () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    await userEvent.click(container.querySelector('button')!);

    expect(container.querySelector('dd')).toHaveClass('mb-4');
  });

  it('does not add mb-4 class to <dd> when collapsed', () => {
    const { container } = render(<StackTraceCard stackTrace={STACK_TRACE} />);

    expect(container.querySelector('dd')).not.toHaveClass('mb-4');
  });
});
