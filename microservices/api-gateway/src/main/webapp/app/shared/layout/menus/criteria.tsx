/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import MenuItem from 'app/shared/layout/menus/menu-item';
import React from 'react';
import { Translate, translate } from 'react-jhipster';

import { NavDropdown } from './menu-components';

export const CriteriaMenu = () => (
  <NavDropdown
    icon="cogs"
    name={translate('global.menu.criteria')}
    id="entity-menu"
    data-testid="criteria-menu"
    style={{ maxHeight: '80vh', overflow: 'auto' }}
  >
    <MenuItem icon="th-list" to="/open-api-criterion">
      <Translate contentKey="global.menu.entities.openApiCriterion" />
    </MenuItem>
  </NavDropdown>
);
