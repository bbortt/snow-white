/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { CodeHighlightBlock } from 'app/entities/quality-gate/code-highlight-block';
import React, { useState } from 'react';
import { Translate } from 'react-jhipster';

import './stack-trace-card.scss';
import { Collapse } from 'reactstrap';

export interface IStackTraceCardProps {
  stackTrace: string;
}

export const StackTraceCard = ({ stackTrace }: IStackTraceCardProps) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="stack-trace-card">
      <dt className="stack-trace-card__toggle">
        <button className="button-no-styles" type="button" onClick={() => setIsOpen(!isOpen)} onKeyDown={() => setIsOpen(!isOpen)}>
          <Translate contentKey="snowWhiteApp.qualityGate.stackTrace">Stack Trace</Translate>
          <FontAwesomeIcon icon={isOpen ? 'chevron-up' : 'chevron-down'} className="stack-trace-card__chevron" />
        </button>
      </dt>
      <dd className={isOpen ? 'mb-4' : ''}>
        <Collapse isOpen={isOpen}>
          <CodeHighlightBlock code={stackTrace} />
        </Collapse>
      </dd>
    </div>
  );
};
