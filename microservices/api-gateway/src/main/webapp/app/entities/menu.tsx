/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import MenuItem from 'app/shared/layout/menus/menu-item';
import React from 'react';
import { Translate } from 'react-jhipster';

const EntitiesMenu = () => {
  return (
    <>
      {/* prettier-ignore */}
      <MenuItem icon="asterisk" to="/quality-gate">
            <Translate contentKey="global.menu.entities.qualityGate" />
        </MenuItem>
      <MenuItem icon="asterisk" to="/quality-gate-config">
        <Translate contentKey="global.menu.entities.qualityGateConfig" />
      </MenuItem>
      <MenuItem icon="asterisk" to="/open-api-criterion">
        <Translate contentKey="global.menu.entities.openApiCriterion" />
      </MenuItem>
      {/* jhipster-needle-add-entity-to-menu - JHipster will add entities to the menu here */}
    </>
  );
};

export default EntitiesMenu;
