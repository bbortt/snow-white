/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ResultFilterCard } from 'app/entities/quality-gate/result-filter-card';
import React from 'react';
import { Translate } from 'react-jhipster';

import QualityGate from './quality-gate';

export const QualityGateResults = () => {
  return (
    <>
      <h2 id="quality-gate-heading" data-cy="QualityGateHeading" className="mb-4">
        <Translate contentKey="snowWhiteApp.qualityGate.home.title">Results</Translate>
      </h2>
      <ResultFilterCard />
      <QualityGate />
    </>
  );
};

export default QualityGateResults;
