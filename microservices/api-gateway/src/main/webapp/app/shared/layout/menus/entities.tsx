/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import EntitiesMenuItems from 'app/entities/menu';
import React from 'react';
import { translate } from 'react-jhipster';

import { NavDropdown } from './menu-components';

export const EntitiesMenu = () => (
  <NavDropdown
    icon="th-list"
    name={translate('global.menu.entities.main')}
    id="entity-menu"
    data-testid="entity"
    style={{ maxHeight: '80vh', overflow: 'auto' }}
  >
    <EntitiesMenuItems />
  </NavDropdown>
);
