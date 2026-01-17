/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { Translate } from 'react-jhipster';
import { Alert } from 'reactstrap';

interface BackendUnavailableBannerProps {
  color?: 'info' | 'danger';
  headerTranslationKey: string;
  bodyTranslationKey: string;
}

const BackendUnavailableBanner: React.FC<BackendUnavailableBannerProps> = ({
  color = 'danger',
  headerTranslationKey,
  bodyTranslationKey,
}: BackendUnavailableBannerProps) => (
  <div id="backend-unavailable-banner">
    <Alert color={color}>
      <h4 className="alert-heading">
        <Translate contentKey={headerTranslationKey}>Snow-White is currently unavailable.</Translate>
      </h4>
      <p>
        <Translate contentKey={bodyTranslationKey}>We’re working to restore service. Please try again later.</Translate>
      </p>
    </Alert>
  </div>
);

export default BackendUnavailableBanner;
