/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import { CoverageProgressBar } from 'app/entities/quality-gate/coverage-progress-bar';
import { IApiTestResult } from 'app/shared/model/api-test-result.model';
import React from 'react';

const result = (coverage?: number): IApiTestResult => ({ isIncludedInQualityGate: true, coverage });

describe('CoverageProgressBar', () => {
  describe('percentage calculation', () => {
    it('should show 100% passed when all results are fully covered', () => {
      render(<CoverageProgressBar apiTestResults={[result(1), result(1), result(1)]} />);

      expect(screen.getByText('100 %')).toBeInTheDocument();
      expect(screen.getByText('0 %')).toBeInTheDocument();
    });

    it('should show 0% passed when all results have no coverage', () => {
      render(<CoverageProgressBar apiTestResults={[result(0), result(0)]} />);

      expect(screen.getByText('0 %')).toBeInTheDocument();
      expect(screen.getByText('100 %')).toBeInTheDocument();
    });

    it('should correctly sum fractional coverage values', () => {
      // 2 results at 0.5 coverage each → passed = 1.0 out of 2 → 50%
      render(<CoverageProgressBar apiTestResults={[result(0.5), result(0.5)]} />);

      const progressElements = screen.getAllByText('50 %');
      expect(progressElements).toHaveLength(2);
      progressElements.forEach(progress => expect(progress).toBeInTheDocument());
    });

    it('should treat missing coverage as 0', () => {
      // 1 result with coverage=1, 1 without → passed = 1 out of 2 → 50%
      render(<CoverageProgressBar apiTestResults={[result(1), result(undefined)]} />);

      const progressElements = screen.getAllByText('50 %');
      expect(progressElements).toHaveLength(2);
      progressElements.forEach(progress => expect(progress).toBeInTheDocument());
    });

    it('should round percentages', () => {
      // 1 result at 0.333 coverage out of 1 → 33%
      render(<CoverageProgressBar apiTestResults={[result(1 / 3)]} />);

      expect(screen.getByText('33 %')).toBeInTheDocument();
      expect(screen.getByText('67 %')).toBeInTheDocument();
    });
  });

  describe('progress bar rendering', () => {
    it('should render two progress bar segments', () => {
      const { container } = render(<CoverageProgressBar apiTestResults={[result(0.8), result(0.6)]} />);

      const bars = container.querySelectorAll('.progress-bar');
      expect(bars).toHaveLength(2);
    });

    it('should render success bar for passed coverage', () => {
      const { container } = render(<CoverageProgressBar apiTestResults={[result(1)]} />);

      const successBar = container.querySelector('.progress-bar.bg-success');
      expect(successBar).toBeInTheDocument();
    });

    it('should render danger bar for failed coverage', () => {
      const { container } = render(<CoverageProgressBar apiTestResults={[result(0)]} />);

      const dangerBar = container.querySelector('.progress-bar.bg-danger');
      expect(dangerBar).toBeInTheDocument();
    });
  });
});
