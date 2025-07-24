/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { IQualityGate } from 'app/shared/model/quality-gate.model';
import React, { useMemo } from 'react';
import { translate } from 'react-jhipster';
import { Badge } from 'reactstrap';

import './status-badge.scss';

type StatusBadgeProps = {
  fill?: boolean;
  qualityGate: IQualityGate;
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({ qualityGate, fill }: StatusBadgeProps) => {
  const status: string = useMemo(() => translate(`snowWhiteApp.ReportStatus.${qualityGate.status}`), [qualityGate.status]);

  const className = fill ? 'badge-block' : '';

  if (qualityGate.status === 'PASSED') {
    return (
      <Badge className={className} color="success">
        <span> {status}</span>
      </Badge>
    );
  } else if (qualityGate.status === 'FAILED') {
    return (
      <Badge className={className} color="danger">
        <span> {status}</span>
      </Badge>
    );
  } else {
    return (
      <Badge className={className} color="primary">
        <span> {status}</span>
      </Badge>
    );
  }
};
