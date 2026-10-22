/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import React, { useMemo } from 'react';
import { translate } from 'react-jhipster';

import './status-badge.scss';
import { Badge } from 'reactstrap';

type StatusBadgeProps = {
  fill?: boolean;
  status: ReportStatus;
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({ fill, status }: StatusBadgeProps) => {
  const className = useMemo(() => (fill ? 'badge-block' : ''), [fill]);
  const statusText: string | null = useMemo(() => translate(`snowWhiteApp.reportStatus.${status}`), [status]);

  if (status === ReportStatus.PASSED) {
    return (
      <Badge className={className} color="success">
        <span>{statusText}</span>
      </Badge>
    );
  } else if (status === ReportStatus.FAILED) {
    return (
      <Badge className={className} color="danger">
        <span>{statusText}</span>
      </Badge>
    );
  } else if (status === ReportStatus.FINISHED_EXCEPTIONALLY) {
    // Distinct from FAILED: this is a run/infrastructure error, not a quality-gate failure.
    return (
      <Badge className={className} color="dark">
        <span>{statusText}</span>
      </Badge>
    );
  } else if (status === ReportStatus.TIMED_OUT) {
    return (
      <Badge className={className} color="warning">
        <span>{statusText}</span>
      </Badge>
    );
  } else {
    return (
      <Badge className={className} color="info">
        <span>{statusText?.startsWith('translation-not-found') ? 'Unknown' : statusText}</span>
      </Badge>
    );
  }
};
