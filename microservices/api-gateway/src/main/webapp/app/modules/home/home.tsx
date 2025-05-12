/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import './home.scss';

import QualityGate from 'app/entities/quality-gate/quality-gate';
import React from 'react';
import { Translate } from 'react-jhipster';
import { Card, CardBody, Col, Row } from 'reactstrap';

export const Home = () => {
  return (
    <Row>
      <Col md="4">
        <Card className="mt-4">
          <CardBody>
            <div className="text-center mb-3">
              <img src="content/images/logo.png" alt="Snow-White Logo" width="80" />
            </div>
            <h5>
              <Translate contentKey="home.title">Welcome to Snow-White</Translate>
            </h5>
            <p>
              <Translate contentKey="home.introduction">
                Snow-White makes API testing effortless by leveraging API specifications and OpenTelemetry (OTEL) data. It works with both
                test environments and live production to analyze:
              </Translate>
            </p>
            <ul>
              <li>
                <Translate contentKey="home.coverage">API Coverage</Translate>
              </li>
              <li>
                <Translate contentKey="home.performance">Performance</Translate>
              </li>
              <li>
                <Translate contentKey="home.insights">And more insights</Translate>
              </li>
            </ul>
          </CardBody>
        </Card>
      </Col>
      <Col md="8">
        <QualityGate />
      </Col>
    </Row>
  );
};

export default Home;
