/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
import { IQualityGate } from 'app/shared/model/quality-gate.model';
import React from 'react';
import { translate } from 'react-jhipster';

jest.mock('react-jhipster', () => ({
  translate: jest.fn().mockImplementation((key: string) => {
    const translations: Record<string, string> = {
      'snowWhiteApp.reportStatus.PASSED': 'PASSED',
      'snowWhiteApp.reportStatus.FAILED': 'FAILED',
      'snowWhiteApp.reportStatus.NOT_STARTED': 'NOT_STARTED',
      'snowWhiteApp.reportStatus.IN_PROGRESS': 'IN_PROGRESS',
      'snowWhiteApp.reportStatus.FINISHED_EXCEPTIONALLY': 'FINISHED_EXCEPTIONALLY',
      'snowWhiteApp.reportStatus.TIMED_OUT': 'TIMED_OUT',
    };
    return translations[key] || key;
  }),
}));

describe('StatusBadge', () => {
  const createQualityGate = (status: string): IQualityGate =>
    ({
      status,
    }) as IQualityGate;

  describe('PASSED status', () => {
    it('should render success badge with correct text for PASSED status', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('PASSED');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-success');
    });

    it('should render success badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('PASSED');
      expect(badge.closest('.badge')).toHaveClass('bg-success', 'badge-block');
    });
  });

  describe('FAILED status', () => {
    it('should render danger badge with correct text for FAILED status', () => {
      const qualityGate = createQualityGate('FAILED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('FAILED');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-danger');
    });

    it('should render danger badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('FAILED');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('FAILED');
      expect(badge.closest('.badge')).toHaveClass('bg-danger', 'badge-block');
    });
  });

  describe('FINISHED_EXCEPTIONALLY status', () => {
    it('should render danger badge with correct text for FINISHED_EXCEPTIONALLY status', () => {
      const qualityGate = createQualityGate('FINISHED_EXCEPTIONALLY');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('FINISHED_EXCEPTIONALLY');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-danger');
    });

    it('should render danger badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('FINISHED_EXCEPTIONALLY');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('FINISHED_EXCEPTIONALLY');
      expect(badge.closest('.badge')).toHaveClass('bg-danger', 'badge-block');
    });
  });

  describe('TIMED_OUT status', () => {
    it('should render danger badge with correct text for TIMED_OUT status', () => {
      const qualityGate = createQualityGate('TIMED_OUT');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('TIMED_OUT');
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-warning');
    });

    it('should render danger badge with fill class when fill prop is true', () => {
      const qualityGate = createQualityGate('TIMED_OUT');

      render(<StatusBadge qualityGate={qualityGate} fill={true} />);

      const badge = screen.getByText('TIMED_OUT');
      expect(badge.closest('.badge')).toHaveClass('bg-warning', 'badge-block');
    });
  });

  describe('Other statuses', () => {
    it.each(['NOT_STARTED', 'IN_PROGRESS'])('should render primary badge for status: %s', (reportStatus: ReportStatus) => {
      const qualityGate = createQualityGate(reportStatus);

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText(reportStatus);
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-info');
    });

    it.each(['NOT_STARTED', 'IN_PROGRESS'])(
      'should render primary badge with fill class for other statuses when fill is true: %s',
      (reportStatus: ReportStatus) => {
        const qualityGate = createQualityGate(reportStatus);

        render(<StatusBadge qualityGate={qualityGate} fill={true} />);

        const badge = screen.getByText(reportStatus);
        expect(badge.closest('.badge')).toHaveClass('bg-info', 'badge-block');
      },
    );
  });

  describe('Translation integration', () => {
    it.each(['PASSED', 'FAILED', 'NOT_STARTED', 'IN_PROGRESS', 'FINISHED_EXCEPTIONALLY', 'TIMED_OUT'])(
      'should call translate with correct key for each status: %s',
      (reportStatus: ReportStatus) => {
        const passedQualityGate = createQualityGate(reportStatus);
        render(<StatusBadge qualityGate={passedQualityGate} />);
        expect(translate).toHaveBeenCalledWith(`snowWhiteApp.reportStatus.${reportStatus}`);
      },
    );
  });

  describe('Edge cases', () => {
    it('should handle unknown translations', () => {
      const status = 'SOMETHING_ELSE';
      const qualityGate = createQualityGate(status);

      (translate as jest.MockedFn<() => string>).mockReturnValueOnce('translation-not-found');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('Unknown');
      expect(badge.closest('.badge')).toHaveClass('bg-info');
    });
  });

  describe('Accessibility', () => {
    it('should render badge with proper structure for screen readers', () => {
      const qualityGate = createQualityGate('PASSED');

      render(<StatusBadge qualityGate={qualityGate} />);

      const badge = screen.getByText('PASSED');
      expect(badge.closest('.badge')).toBeInTheDocument();
      // Badge should be accessible to screen readers
      expect(badge).toBeVisible();
    });
  });
});
