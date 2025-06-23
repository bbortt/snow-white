/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './header.scss';

import { useAppDispatch } from 'app/config/store';
import { ResourcesMenu } from 'app/shared/layout/menus/resources';
import { setLocale } from 'app/shared/reducers/locale';
import React, { useState } from 'react';
import { Storage, Translate } from 'react-jhipster';
import LoadingBar from 'react-redux-loading-bar';
import { Collapse, Nav, Navbar, NavbarToggler } from 'reactstrap';

import { CriteriaMenu, LocaleMenu } from '../menus';
import { Brand, Home, QualityGates } from './header-components';

export interface IHeaderProps {
  ribbonEnv: string;
  isInProduction: boolean;
  isOpenAPIEnabled: boolean;
  currentLocale: string;
}

const Header = (props: IHeaderProps) => {
  const [menuOpen, setMenuOpen] = useState(false);

  const dispatch = useAppDispatch();

  const handleLocaleChange = event => {
    const langKey = event.target.value;
    Storage.session.set('locale', langKey);
    dispatch(setLocale(langKey));
  };

  const renderDevRibbon = () =>
    !props.isInProduction ? (
      <div className="ribbon dev" data-testid="dev-ribbon">
        <a href="">
          <Translate contentKey={`global.ribbon.${props.ribbonEnv}`} />
        </a>
      </div>
    ) : null;

  const toggleMenu = () => {
    setMenuOpen(!menuOpen);
  };

  /* jhipster-needle-add-element-to-menu - JHipster will add new menu items here */

  return (
    <div id="app-header">
      {renderDevRibbon()}
      <LoadingBar className="loading-bar" />
      <Navbar data-testid="navbar" dark expand="md" fixed="top" className="jh-navbar">
        <NavbarToggler aria-label="Menu" onClick={toggleMenu} />
        <Brand />
        <Collapse isOpen={menuOpen} navbar>
          <Nav id="header-tabs" className="ms-auto" navbar>
            <Home />
            <QualityGates />
            <CriteriaMenu />
            <LocaleMenu currentLocale={props.currentLocale} onClick={handleLocaleChange} />
            <ResourcesMenu />
          </Nav>
        </Collapse>
      </Navbar>
    </div>
  );
};

export default Header;
