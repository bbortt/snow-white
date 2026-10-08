/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import type { IApiTestFinding, IFindingEvidence } from 'app/shared/model/api-test-finding.model';

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { FindingStatus } from 'app/shared/model/enumerations/finding-status.model';
import React, { useEffect, useId, useState } from 'react';
import { translate } from 'react-jhipster';
import { Badge, Button, Collapse } from 'reactstrap';

// Uncovered first: that's what a user drilling into a low coverage number is after.
const STATUS_ORDER = [FindingStatus.UNCOVERED, FindingStatus.COVERED, FindingStatus.NOT_APPLICABLE];
const INITIAL_VISIBLE_FINDINGS = 20;
const VISIBLE_UNNAMED_TRACES = 3;
const SHORT_TRACE_ID_LENGTH = 8;
const COPIED_FEEDBACK_MILLIS = 2000;

const findingKey = ({ specPointer, httpMethod, responseCode, parameterName, contentType }: IApiTestFinding): string =>
  [specPointer, httpMethod, responseCode, parameterName, contentType].join('|');

const CopyButton: React.FC<{ value: string }> = ({ value }) => {
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) {
      return undefined;
    }
    const timeout = setTimeout(() => setCopied(false), COPIED_FEEDBACK_MILLIS);
    return () => clearTimeout(timeout);
  }, [copied]);

  const copy = () => navigator.clipboard?.writeText(value).then(() => setCopied(true));

  return (
    <Button
      color="link"
      size="sm"
      className="p-0 ms-1 text-muted"
      onClick={copy}
      aria-label={translate(copied ? 'snowWhiteApp.apiTestResult.findings.copied' : 'snowWhiteApp.apiTestResult.findings.copy')}
    >
      <FontAwesomeIcon icon={copied ? 'check-circle' : 'copy'} aria-hidden="true" />
    </Button>
  );
};

const TraceId: React.FC<{ traceId: string }> = ({ traceId }) => (
  <span className="text-nowrap">
    <code title={traceId} aria-label={translate('snowWhiteApp.apiTestResult.findings.trace', { traceId })}>
      {traceId.slice(0, SHORT_TRACE_ID_LENGTH)}…
    </code>
    <CopyButton value={traceId} />
  </span>
);

const Evidence: React.FC<{ evidence: IFindingEvidence[] }> = ({ evidence }) => {
  const tracesByTestCase = new Map<string, IFindingEvidence[]>();
  const unnamedTraces: IFindingEvidence[] = [];
  evidence.forEach(trace => {
    if (trace.testCaseName) {
      tracesByTestCase.set(trace.testCaseName, [...(tracesByTestCase.get(trace.testCaseName) ?? []), trace]);
    } else {
      unnamedTraces.push(trace);
    }
  });
  const hiddenUnnamedTraces = unnamedTraces.length - VISIBLE_UNNAMED_TRACES;

  return (
    <ul className="list-inline small mb-0" data-cy="findingEvidence">
      {Array.from(tracesByTestCase, ([testCaseName, traces]) => (
        <li key={testCaseName} className="list-inline-item">
          <span className="font-monospace">{testCaseName}</span>
          {traces.length > 1 ? (
            <span className="text-muted ms-1">({translate('snowWhiteApp.apiTestResult.findings.traces', { count: traces.length })})</span>
          ) : null}
        </li>
      ))}
      {unnamedTraces.slice(0, VISIBLE_UNNAMED_TRACES).map(trace => (
        <li key={trace.traceId} className="list-inline-item">
          <TraceId traceId={trace.traceId} />
        </li>
      ))}
      {hiddenUnnamedTraces > 0 ? (
        <li className="list-inline-item text-muted">
          {translate('snowWhiteApp.apiTestResult.findings.moreTraces', { count: hiddenUnnamedTraces })}
        </li>
      ) : null}
    </ul>
  );
};

const Finding: React.FC<{ finding: IApiTestFinding }> = ({ finding }) => (
  <li className="py-2 border-bottom" data-cy="finding">
    <div className="d-flex flex-wrap align-items-center gap-1">
      {finding.httpMethod ? (
        <Badge color="dark" className="font-monospace">
          {finding.httpMethod}
        </Badge>
      ) : null}
      {finding.httpPath ? <code className="text-break">{finding.httpPath}</code> : null}
      {finding.responseCode ? (
        <Badge color="light" className="text-dark border">
          {translate('snowWhiteApp.apiTestResult.findings.responseCode')} {finding.responseCode}
        </Badge>
      ) : null}
      {finding.parameterName ? (
        <Badge color="light" className="text-dark border">
          {translate('snowWhiteApp.apiTestResult.findings.parameter')} <span className="font-monospace">{finding.parameterName}</span>
        </Badge>
      ) : null}
      {finding.contentType ? (
        <Badge color="light" className="text-dark border">
          {translate('snowWhiteApp.apiTestResult.findings.contentType')} <span className="font-monospace">{finding.contentType}</span>
        </Badge>
      ) : null}
    </div>
    <div className="small text-muted text-break">
      <code className="text-muted">{finding.specPointer}</code>
      <CopyButton value={finding.specPointer} />
    </div>
    {finding.status === FindingStatus.COVERED && finding.evidence.length > 0 ? <Evidence evidence={finding.evidence} /> : null}
  </li>
);

const FindingSection: React.FC<{ status: FindingStatus; findings: IApiTestFinding[] }> = ({ status, findings }) => {
  const contentId = useId();
  // Not-applicable findings explain nothing about a coverage gap, so they start collapsed.
  const [isOpen, setIsOpen] = useState(status !== FindingStatus.NOT_APPLICABLE);
  const [showAll, setShowAll] = useState(false);

  const visibleFindings = showAll ? findings : findings.slice(0, INITIAL_VISIBLE_FINDINGS);

  return (
    <section data-cy={`findings-${status}`}>
      <h6 className="mb-0 mt-2">
        <Button
          color="link"
          className="p-0 text-reset text-decoration-none"
          onClick={() => setIsOpen(!isOpen)}
          aria-expanded={isOpen}
          aria-controls={contentId}
        >
          <FontAwesomeIcon icon={isOpen ? 'chevron-up' : 'chevron-down'} className="me-2" aria-hidden="true" />
          {translate(`snowWhiteApp.apiTestResult.findings.status.${status}`)} ({findings.length})
        </Button>
      </h6>
      <Collapse isOpen={isOpen} id={contentId}>
        <ul className="list-unstyled mb-0 ps-4">
          {visibleFindings.map(finding => (
            <Finding key={findingKey(finding)} finding={finding} />
          ))}
        </ul>
        {visibleFindings.length < findings.length ? (
          <Button color="link" size="sm" className="ps-4" onClick={() => setShowAll(true)}>
            {translate('snowWhiteApp.apiTestResult.findings.showAll', { count: findings.length })}
          </Button>
        ) : null}
      </Collapse>
    </section>
  );
};

interface ApiTestFindingsProps {
  findings: IApiTestFinding[];
}

export const ApiTestFindings: React.FC<ApiTestFindingsProps> = ({ findings }: ApiTestFindingsProps) => (
  <div data-cy="apiTestFindings">
    {STATUS_ORDER.map(status => {
      const findingsWithStatus = findings.filter(finding => finding.status === status);
      return findingsWithStatus.length > 0 ? <FindingSection key={status} status={status} findings={findingsWithStatus} /> : null;
    })}
  </div>
);

export default ApiTestFindings;
