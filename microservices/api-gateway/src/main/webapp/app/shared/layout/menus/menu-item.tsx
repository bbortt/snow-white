/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';
import { DropdownItem } from 'reactstrap';
import { NavLink as Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { IconProp } from '@fortawesome/fontawesome-svg-core';

export interface IMenuItem {
  children: React.ReactNode;
  icon: IconProp;
  to: string;
  id?: string;
  'data-cy'?: string;
}

const MenuItem: React.FC<IMenuItem> = (props: IMenuItem) => {
  const { to, icon, id, children } = props;

  return (
    <DropdownItem tag={Link} to={to} id={id} data-cy={props['data-cy']}>
      <FontAwesomeIcon icon={icon} fixedWidth /> {children}
    </DropdownItem>
  );
};

export default MenuItem;
