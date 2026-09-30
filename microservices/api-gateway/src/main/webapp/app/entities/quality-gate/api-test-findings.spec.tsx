/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestFinding } from 'app/shared/model/api-test-finding.model';

import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { ApiTestFindings } from 'app/entities/quality-gate/api-test-findings';
import { FindingStatus } from 'app/shared/model/enumerations/finding-status.model';
import React from 'react';

jest.mock('react-jhipster', () => ({
  translate: (key: string, interpolate?: Record<string, unknown>) => (interpolate ? `${key}:${Object.values(interpolate).join(',')}` : key),
}));

describe('ApiTestFindings', () => {
  const finding = (overrides: Partial<IApiTestFinding> = {}): IApiTestFinding => ({
    status: FindingStatus.UNCOVERED,
    specPointer: '#/paths/~1orders/get',
    evidence: [],
    ...overrides,
  });

  const sectionToggle = (status: FindingStatus) =>
    within(document.querySelector(`[data-cy="findings-${status}"]`) as HTMLElement).getAllByRole('button')[0];

  it('should list uncovered findings first and not-applicable ones last', () => {
    render(
      <ApiTestFindings
        findings={[
          finding({ status: FindingStatus.NOT_APPLICABLE, specPointer: '#/a' }),
          finding({ status: FindingStatus.COVERED, specPointer: '#/b' }),
          finding({ status: FindingStatus.UNCOVERED, specPointer: '#/c' }),
          finding({ status: FindingStatus.UNCOVERED, specPointer: '#/d' }),
        ]}
      />,
    );

    const sections = Array.from(document.querySelectorAll('[data-cy^="findings-"]')).map(section => section.getAttribute('data-cy'));
    expect(sections).toEqual(['findings-UNCOVERED', 'findings-COVERED', 'findings-NOT_APPLICABLE']);
    expect(sectionToggle(FindingStatus.UNCOVERED)).toHaveTextContent('snowWhiteApp.apiTestResult.findings.status.UNCOVERED (2)');
  });

  it('should leave out a status no finding has', () => {
    render(<ApiTestFindings findings={[finding()]} />);

    expect(document.querySelector('[data-cy="findings-COVERED"]')).not.toBeInTheDocument();
  });

  it('should collapse not-applicable findings until asked for', () => {
    render(
      <ApiTestFindings
        findings={[finding({ status: FindingStatus.UNCOVERED }), finding({ status: FindingStatus.NOT_APPLICABLE, specPointer: '#/n' })]}
      />,
    );

    expect(sectionToggle(FindingStatus.UNCOVERED)).toHaveAttribute('aria-expanded', 'true');
    const notApplicable = sectionToggle(FindingStatus.NOT_APPLICABLE);
    expect(notApplicable).toHaveAttribute('aria-expanded', 'false');

    fireEvent.click(notApplicable);

    expect(notApplicable).toHaveAttribute('aria-expanded', 'true');
  });

  it('should spell out only the dimensions a finding has', () => {
    render(
      <ApiTestFindings
        findings={[
          finding({ httpMethod: 'GET', httpPath: '/orders', responseCode: '400', specPointer: '#/paths/~1orders/get/responses/400' }),
        ]}
      />,
    );

    const row = screen.getByText('/orders').closest('li') as HTMLElement;
    expect(row).toHaveTextContent('GET');
    expect(row).toHaveTextContent('snowWhiteApp.apiTestResult.findings.responseCode 400');
    expect(row).toHaveTextContent('#/paths/~1orders/get/responses/400');
    expect(row).not.toHaveTextContent('snowWhiteApp.apiTestResult.findings.parameter');
    expect(row).not.toHaveTextContent('snowWhiteApp.apiTestResult.findings.contentType');
  });

  it('should name the test behind covered evidence and count its traces', () => {
    render(
      <ApiTestFindings
        findings={[
          finding({
            status: FindingStatus.COVERED,
            evidence: [
              { traceId: '11111111aaaaaaaa', testCaseName: 'OrderApiIT#listsOrders' },
              { traceId: '22222222bbbbbbbb', testCaseName: 'OrderApiIT#listsOrders' },
            ],
          }),
        ]}
      />,
    );

    const evidence = document.querySelector('[data-cy="findingEvidence"]') as HTMLElement;
    expect(evidence).toHaveTextContent('OrderApiIT#listsOrders');
    expect(evidence).toHaveTextContent('snowWhiteApp.apiTestResult.findings.traces:2');
    expect(evidence).not.toHaveTextContent('11111111');
  });

  it('should fall back to a shortened trace id, and count the traces it leaves out', () => {
    render(
      <ApiTestFindings
        findings={[
          finding({
            status: FindingStatus.COVERED,
            evidence: ['aaaaaaaa1', 'bbbbbbbb2', 'cccccccc3', 'dddddddd4', 'eeeeeeee5'].map(traceId => ({ traceId })),
          }),
        ]}
      />,
    );

    const evidence = document.querySelector('[data-cy="findingEvidence"]') as HTMLElement;
    const shownTrace = within(evidence).getByTitle('aaaaaaaa1');
    expect(shownTrace).toHaveTextContent('aaaaaaaa…');
    expect(shownTrace).not.toHaveTextContent('aaaaaaaa1');
    expect(within(evidence).queryByTitle('dddddddd4')).not.toBeInTheDocument();
    expect(evidence).toHaveTextContent('snowWhiteApp.apiTestResult.findings.moreTraces:2');
  });

  it('should list no evidence for an uncovered finding', () => {
    render(<ApiTestFindings findings={[finding({ evidence: [{ traceId: 'aaaaaaaa1' }] })]} />);

    expect(document.querySelector('[data-cy="findingEvidence"]')).not.toBeInTheDocument();
  });

  it('should show the first twenty findings of a status, and the rest on request', () => {
    const findings = Array.from({ length: 23 }, (_, index) => finding({ specPointer: `#/paths/${index}` }));
    render(<ApiTestFindings findings={findings} />);

    expect(document.querySelectorAll('[data-cy="finding"]')).toHaveLength(20);

    fireEvent.click(screen.getByRole('button', { name: 'snowWhiteApp.apiTestResult.findings.showAll:23' }));

    expect(document.querySelectorAll('[data-cy="finding"]')).toHaveLength(23);
  });

  it('should copy the spec pointer to the clipboard', async () => {
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.assign(navigator, { clipboard: { writeText } });
    render(<ApiTestFindings findings={[finding()]} />);

    fireEvent.click(screen.getByRole('button', { name: 'snowWhiteApp.apiTestResult.findings.copy' }));

    expect(writeText).toHaveBeenCalledWith('#/paths/~1orders/get');
    await waitFor(() => expect(screen.getByRole('button', { name: 'snowWhiteApp.apiTestResult.findings.copied' })).toBeInTheDocument());
  });
});
