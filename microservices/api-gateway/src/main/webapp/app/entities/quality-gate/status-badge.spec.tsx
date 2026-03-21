/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render, screen } from '@testing-library/react';
import { StatusBadge } from 'app/entities/quality-gate/status-badge';
import { ReportStatus } from 'app/shared/model/enumerations/report-status.model';
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
  describe.each([
    { reportStatus: ReportStatus.PASSED, background: 'bg-success' },
    { reportStatus: ReportStatus.FAILED, background: 'bg-danger' },
    { reportStatus: ReportStatus.FINISHED_EXCEPTIONALLY, background: 'bg-danger' },
    { reportStatus: ReportStatus.TIMED_OUT, background: 'bg-warning' },
  ])('for status: %s', ({ reportStatus, background }: { reportStatus: ReportStatus; background: string }) => {
    it('should render badge with correct text', () => {
      render(<StatusBadge status={reportStatus} />);

      const badge = screen.getByText(reportStatus);
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass(background);
    });

    it('should render badge with fill class when fill prop is true', () => {
      render(<StatusBadge status={reportStatus} fill={true} />);

      const badge = screen.getByText(reportStatus);
      expect(badge.closest('.badge')).toHaveClass(background, 'badge-block');
    });
  });

  describe.each([ReportStatus.NOT_STARTED, ReportStatus.IN_PROGRESS])('for status: %s', (reportStatus: ReportStatus) => {
    it('should render primary badge', () => {
      render(<StatusBadge status={reportStatus} />);

      const badge = screen.getByText(reportStatus);
      expect(badge).toBeInTheDocument();
      expect(badge.closest('.badge')).toHaveClass('bg-info');
    });

    it('should render primary badge with fill class when fill is true', () => {
      render(<StatusBadge status={reportStatus} fill={true} />);

      const badge = screen.getByText(reportStatus);
      expect(badge.closest('.badge')).toHaveClass('bg-info', 'badge-block');
    });
  });

  describe('Translation integration', () => {
    it.each(Object.entries(ReportStatus))('should call translate with correct key for each status: %s', (reportStatus: ReportStatus) => {
      render(<StatusBadge status={reportStatus} />);
      expect(translate).toHaveBeenCalledWith(`snowWhiteApp.reportStatus.${reportStatus}`);
    });
  });

  describe('Accessibility', () => {
    it('should render badge with proper structure for screen readers', () => {
      render(<StatusBadge status={ReportStatus.PASSED} />);

      const badge = screen.getByText('PASSED');
      expect(badge.closest('.badge')).toBeInTheDocument();
      // Badge should be accessible to screen readers
      expect(badge).toBeVisible();
    });
  });
});
