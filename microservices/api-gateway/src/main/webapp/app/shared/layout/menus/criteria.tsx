/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { Translate, translate } from 'react-jhipster';
import { NavDropdown } from './menu-components';
import MenuItem from 'app/shared/layout/menus/menu-item';

export const CriteriaMenu = () => (
  <NavDropdown
    icon="cogs"
    name={translate('global.menu.criteria')}
    id="entity-menu"
    data-cy="entity"
    style={{ maxHeight: '80vh', overflow: 'auto' }}
  >
    <MenuItem icon="th-list" to="/open-api-criterion">
      <Translate contentKey="global.menu.entities.openApiCriterion" />
    </MenuItem>
  </NavDropdown>
);
