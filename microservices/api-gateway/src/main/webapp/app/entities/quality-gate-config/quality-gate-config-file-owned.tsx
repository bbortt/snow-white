/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { Translate } from 'react-jhipster';

export const FILE_OWNED_REASON_KEY = 'snowWhiteApp.qualityGateConfig.fileOwned';
export const FILE_OWNED_REASON_ID = 'quality-gate-config-file-owned';

/** Why no gate can be written here: the instance's gates come from its gate file. */
export const QualityGateConfigFileOwned = () => (
  <div className="alert alert-info" id={FILE_OWNED_REASON_ID} data-testid="qualityGateConfigFileOwned">
    <Translate contentKey={FILE_OWNED_REASON_KEY}>
      This instance&apos;s Quality-Gates are defined in its gate file. They cannot be created, edited or deleted here; change the gate file
      instead.
    </Translate>
  </div>
);
