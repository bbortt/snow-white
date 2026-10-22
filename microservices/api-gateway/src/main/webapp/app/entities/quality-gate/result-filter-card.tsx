/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { apiIndexApi } from 'app/entities/api-index/api-index-api';
import { uniqueSortedVersions } from 'app/entities/api-index/api-index.utils';
import { AutocompleteInput } from 'app/entities/api-index/autocomplete-input';
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Translate, translate } from 'react-jhipster';
import { useLocation, useNavigate } from 'react-router';
import { Badge, Button, Card, CardBody, CardHeader, Col, Collapse, Label, Row } from 'reactstrap';

import { countActiveFilters } from './quality-gate.utils';

export const ResultFilterCard = () => {
  const pageLocation = useLocation();
  const navigate = useNavigate();

  // Immediate input state — initialized from URL so back/forward and shared links work
  const [inputServiceName, setInputServiceName] = useState(() => new URLSearchParams(pageLocation.search).get('serviceName') ?? '');
  const [inputApiName, setInputApiName] = useState(() => new URLSearchParams(pageLocation.search).get('apiName') ?? '');
  const [inputApiVersion, setInputApiVersion] = useState(() => new URLSearchParams(pageLocation.search).get('apiVersion') ?? '');

  // Debounced state driving URL updates
  const [filterServiceName, setFilterServiceName] = useState(() => new URLSearchParams(pageLocation.search).get('serviceName') ?? '');
  const [filterApiName, setFilterApiName] = useState(() => new URLSearchParams(pageLocation.search).get('apiName') ?? '');
  const [filterApiVersion, setFilterApiVersion] = useState(() => new URLSearchParams(pageLocation.search).get('apiVersion') ?? '');

  const activeCount = useMemo(
    () => countActiveFilters({ serviceName: filterServiceName, apiName: filterApiName, apiVersion: filterApiVersion }),
    [filterServiceName, filterApiName, filterApiVersion],
  );

  const [isOpen, setIsOpen] = useState(() => activeCount > 0);

  const [serviceNames, setServiceNames] = useState<string[]>([]);
  const [apiNames, setApiNames] = useState<string[]>([]);
  const [apiVersions, setApiVersions] = useState<string[]>([]);

  // Refs to skip the debounce cascade on initial mount and on URL-driven syncs
  const prevInputServiceNameRef = useRef(inputServiceName);
  const prevInputApiNameRef = useRef(inputApiName);
  const prevInputApiVersionRef = useRef(inputApiVersion);

  // Tracks the previous debounced filter state to skip URL navigation when nothing changed
  const prevFilterRef = useRef({ serviceName: filterServiceName, apiName: filterApiName, apiVersion: filterApiVersion });

  // Tracks the current URL so filter navigation preserves existing sort params
  const currentSearchRef = useRef(pageLocation.search);
  currentSearchRef.current = pageLocation.search;

  // Sync all input/filter state from URL on external navigation (back/forward, shared links)
  useEffect(() => {
    const params = new URLSearchParams(pageLocation.search);
    const urlServiceName = params.get('serviceName') ?? '';
    const urlApiName = params.get('apiName') ?? '';
    const urlApiVersion = params.get('apiVersion') ?? '';

    // Update prevRefs before state so debounce effects see no change and skip their cascade
    prevInputServiceNameRef.current = urlServiceName;
    prevInputApiNameRef.current = urlApiName;
    prevInputApiVersionRef.current = urlApiVersion;
    prevFilterRef.current = { serviceName: urlServiceName, apiName: urlApiName, apiVersion: urlApiVersion };

    setInputServiceName(urlServiceName);
    setFilterServiceName(urlServiceName);
    setInputApiName(urlApiName);
    setFilterApiName(urlApiName);
    setInputApiVersion(urlApiVersion);
    setFilterApiVersion(urlApiVersion);
  }, [pageLocation.search]);

  // Debounce service name — also resets the API name and version cascades
  useEffect(() => {
    const prev = prevInputServiceNameRef.current;
    prevInputServiceNameRef.current = inputServiceName;
    if (prev === inputServiceName) return;
    const t = setTimeout(() => {
      setFilterServiceName(inputServiceName);
      prevInputApiNameRef.current = '';
      prevInputApiVersionRef.current = '';
      setInputApiName('');
      setFilterApiName('');
      setInputApiVersion('');
      setFilterApiVersion('');
    }, 300);
    return () => clearTimeout(t);
  }, [inputServiceName]);

  // Debounce API name — also resets the version cascade
  useEffect(() => {
    const prev = prevInputApiNameRef.current;
    prevInputApiNameRef.current = inputApiName;
    if (prev === inputApiName) return;
    const t = setTimeout(() => {
      setFilterApiName(inputApiName);
      prevInputApiVersionRef.current = '';
      setInputApiVersion('');
      setFilterApiVersion('');
    }, 300);
    return () => clearTimeout(t);
  }, [inputApiName]);

  // Debounce API version
  useEffect(() => {
    const prev = prevInputApiVersionRef.current;
    prevInputApiVersionRef.current = inputApiVersion;
    if (prev === inputApiVersion) return;
    const t = setTimeout(() => setFilterApiVersion(inputApiVersion), 300);
    return () => clearTimeout(t);
  }, [inputApiVersion]);

  // Push filter changes to URL, resetting page to 1 and preserving existing sort param
  useEffect(() => {
    const prev = prevFilterRef.current;
    prevFilterRef.current = { serviceName: filterServiceName, apiName: filterApiName, apiVersion: filterApiVersion };
    if (prev.serviceName === filterServiceName && prev.apiName === filterApiName && prev.apiVersion === filterApiVersion) {
      return;
    }

    const params = new URLSearchParams(currentSearchRef.current);
    if (filterServiceName) params.set('serviceName', filterServiceName);
    else params.delete('serviceName');
    if (filterApiName) params.set('apiName', filterApiName);
    else params.delete('apiName');
    if (filterApiVersion) params.set('apiVersion', filterApiVersion);
    else params.delete('apiVersion');
    params.set('page', '1');

    navigate(`${pageLocation.pathname}?${params.toString()}`);
  }, [filterServiceName, filterApiName, filterApiVersion]);

  // Fetch service name suggestions on mount
  useEffect(() => {
    apiIndexApi.getAllServiceNames().then(res => setServiceNames(res.data));
  }, []);

  // Fetch API name suggestions whenever the service name filter changes (cascading)
  useEffect(() => {
    apiIndexApi.getAllApiNames(filterServiceName || undefined).then(res => setApiNames(res.data));
  }, [filterServiceName]);

  // Fetch API version suggestions from the index when service or API name is set
  useEffect(() => {
    if (!filterServiceName && !filterApiName) {
      setApiVersions([]);
      return;
    }
    apiIndexApi
      .getAllApis(0, 200, undefined, filterServiceName || undefined, filterApiName || undefined)
      .then(res => setApiVersions(uniqueSortedVersions(res.data.map(api => api.apiVersion))));
  }, [filterServiceName, filterApiName]);

  const handleClearFilters = (e: React.MouseEvent) => {
    e.stopPropagation();
    prevInputServiceNameRef.current = '';
    prevInputApiNameRef.current = '';
    prevInputApiVersionRef.current = '';
    prevFilterRef.current = { serviceName: '', apiName: '', apiVersion: '' };
    setInputServiceName('');
    setInputApiName('');
    setInputApiVersion('');
    setFilterServiceName('');
    setFilterApiName('');
    setFilterApiVersion('');

    const params = new URLSearchParams(currentSearchRef.current);
    params.delete('serviceName');
    params.delete('apiName');
    params.delete('apiVersion');
    params.set('page', '1');
    navigate(`${pageLocation.pathname}?${params.toString()}`);
  };

  const toggleCard = () => setIsOpen(open => !open);

  return (
    <Card className="mb-3">
      <CardHeader role="button" onClick={toggleCard} className="d-flex justify-content-between align-items-center">
        <span>
          <Translate contentKey="snowWhiteApp.qualityGate.filter.title">Filters</Translate>
          {activeCount > 0 && (
            <Badge color="primary" pill className="ms-2">
              {activeCount}
            </Badge>
          )}
        </span>
        <div className="d-flex align-items-center gap-2">
          {activeCount > 0 && (
            <Button color="link" size="sm" className="p-0 text-muted" onClick={handleClearFilters}>
              <Translate contentKey="snowWhiteApp.qualityGate.filter.clear">Clear</Translate>
            </Button>
          )}
          <FontAwesomeIcon icon={isOpen ? 'chevron-up' : 'chevron-down'} />
        </div>
      </CardHeader>
      <Collapse isOpen={isOpen}>
        <CardBody>
          <Row>
            <Col md={4}>
              <Label>
                <Translate contentKey="snowWhiteApp.qualityGate.filter.serviceName">Service Name</Translate>
              </Label>
              <AutocompleteInput
                value={inputServiceName}
                onChange={setInputServiceName}
                suggestions={serviceNames}
                placeholder={translate('snowWhiteApp.qualityGate.filter.serviceName', {}, 'Service Name')}
              />
            </Col>
            <Col md={4}>
              <Label>
                <Translate contentKey="snowWhiteApp.qualityGate.filter.apiName">API Name</Translate>
              </Label>
              <AutocompleteInput
                value={inputApiName}
                onChange={setInputApiName}
                suggestions={apiNames}
                placeholder={translate('snowWhiteApp.qualityGate.filter.apiName', {}, 'API Name')}
              />
            </Col>
            <Col md={4}>
              <Label>
                <Translate contentKey="snowWhiteApp.qualityGate.filter.apiVersion">Version</Translate>
              </Label>
              <AutocompleteInput
                value={inputApiVersion}
                onChange={setInputApiVersion}
                suggestions={apiVersions}
                placeholder={translate('snowWhiteApp.qualityGate.filter.apiVersion', {}, 'Version')}
              />
            </Col>
          </Row>
        </CardBody>
      </Collapse>
    </Card>
  );
};
