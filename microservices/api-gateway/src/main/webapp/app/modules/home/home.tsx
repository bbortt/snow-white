/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './home.scss';

import React from 'react';
import { Translate } from 'react-jhipster';
import { Col, Row } from 'reactstrap';
import QualityGate from 'app/entities/quality-gate/quality-gate';

export const Home = () => {
  return (
    <Row>
      <Col md="4">
        <h1 className="display-4">
          <Translate contentKey="home.title">Welcome, Java Hipster!</Translate>
        </h1>
      </Col>
        <Col md="6">
        <QualityGate />
      </Col>
    </Row>
  );
};

export default Home;
