import {
  ChartVersionBumper,
  FileSystem,
  Logger,
  runCLI,
} from '../scripts/bump-chart-version';

describe('bump-chart-version.ts', () => {
  let mockFs: FileSystem;
  let mockLogger: Logger;
  let bumper: ChartVersionBumper;

  beforeEach(() => {
    mockFs = {
      readFileSync: vi.fn(),
      writeFileSync: vi.fn(),
    };
    mockLogger = {
      log: vi.fn(),
      error: vi.fn(),
    };
    bumper = new ChartVersionBumper(mockFs, mockLogger);
  });

  describe('validateVersion', () => {
    it('should return trimmed version for valid input', () => {
      expect(bumper.validateVersion('1.2.3')).toBe('1.2.3');
      expect(bumper.validateVersion('  1.2.3  ')).toBe('1.2.3');
    });

    it('should throw error for undefined version', () => {
      expect(() => bumper.validateVersion(undefined)).toThrow(
        'Version is required',
      );
    });

    it('should throw error for empty version', () => {
      expect(() => bumper.validateVersion('')).toThrow('Version is required');
      expect(() => bumper.validateVersion('   ')).toThrow(
        'Version is required',
      );
    });
  });

  describe('bumpChartVersion', () => {
    const mockChartContent = `
name: snow-white
version: 1.0.0
appVersion: 0.1.0
description: A Helm chart for snow-white
`;

    beforeEach(() => {
      vi.mocked(mockFs.readFileSync).mockReturnValue(mockChartContent);
    });

    it('should successfully update chart version', () => {
      bumper.bumpChartVersion('2.0.0');

      expect(mockFs.readFileSync).toHaveBeenCalledWith(
        'charts/snow-white/Chart.yaml',
        'utf8',
      );
      expect(mockFs.writeFileSync).toHaveBeenCalledWith(
        'charts/snow-white/Chart.yaml',
        expect.stringContaining('version: 2.0.0'),
        'utf8',
      );
      expect(mockLogger.log).toHaveBeenCalledWith(
        "Successfully updated appVersion to '2.0.0' in charts/snow-white/Chart.yaml",
      );
    });

    it('should successfully update app version', () => {
      bumper.bumpChartVersion('2.0.0');

      expect(mockFs.readFileSync).toHaveBeenCalledWith(
        'charts/snow-white/Chart.yaml',
        'utf8',
      );
      expect(mockFs.writeFileSync).toHaveBeenCalledWith(
        'charts/snow-white/Chart.yaml',
        expect.stringContaining('appVersion: 2.0.0'),
        'utf8',
      );
      expect(mockLogger.log).toHaveBeenCalledWith(
        "Successfully updated appVersion to '2.0.0' in charts/snow-white/Chart.yaml",
      );
    });

    it('should use custom chart path when provided', () => {
      const customPath = 'custom/path/Chart.yaml';
      bumper.bumpChartVersion('2.0.0', customPath);

      expect(mockFs.readFileSync).toHaveBeenCalledWith(customPath, 'utf8');
      expect(mockFs.writeFileSync).toHaveBeenCalledWith(
        customPath,
        expect.any(String),
        'utf8',
      );
      expect(mockLogger.log).toHaveBeenCalledWith(
        `Successfully updated appVersion to '2.0.0' in ${customPath}`,
      );
    });

    it('should handle file read errors', () => {
      const readError = new Error('File not found');
      vi.mocked(mockFs.readFileSync).mockImplementation(() => {
        throw readError;
      });

      expect(() => bumper.bumpChartVersion('2.0.0')).toThrow('File not found');
      expect(mockLogger.error).toHaveBeenCalledWith(
        'Error updating chart version: File not found',
      );
    });

    it('should handle file write errors', () => {
      const writeError = new Error('Permission denied');
      vi.mocked(mockFs.writeFileSync).mockImplementation(() => {
        throw writeError;
      });

      expect(() => bumper.bumpChartVersion('2.0.0')).toThrow(
        'Permission denied',
      );
      expect(mockLogger.error).toHaveBeenCalledWith(
        'Error updating chart version: Permission denied',
      );
    });

    it('should handle invalid YAML content', () => {
      vi.mocked(mockFs.readFileSync).mockReturnValue(
        'invalid: yaml: content: [',
      );

      expect(() => bumper.bumpChartVersion('2.0.0')).toThrow();
      expect(mockLogger.error).toHaveBeenCalledWith(
        expect.stringMatching(/Error updating chart version:/),
      );
    });

    it.each(['', ' '])(
      'should handle erroneous parsed content: %s',
      (parsedContent: string) => {
        vi.mocked(mockFs.readFileSync).mockReturnValue(parsedContent);

        expect(() => bumper.bumpChartVersion('2.0.0')).toThrow(
          'Invalid YAML structure in chart file',
        );
        expect(mockLogger.error).toHaveBeenCalledWith(
          'Error updating chart version: Invalid YAML structure in chart file',
        );
      },
    );

    it('should handle non-Error exceptions', () => {
      vi.mocked(mockFs.readFileSync).mockImplementation(() => {
        throw 'String error';
      });

      expect(() => bumper.bumpChartVersion('2.0.0')).toThrow();
      expect(mockLogger.error).toHaveBeenCalledWith(
        'Error updating chart version: String error',
      );
    });

    it('should trim whitespace from version', () => {
      bumper.bumpChartVersion('  2.0.0  ');

      expect(mockFs.writeFileSync).toHaveBeenCalledWith(
        'charts/snow-white/Chart.yaml',
        expect.stringContaining('appVersion: 2.0.0'),
        'utf8',
      );
    });
  });

  describe('integration with real YAML parsing', () => {
    it('should preserve other YAML properties', () => {
      const complexChart = `name: snow-white
version: 1.0.0
appVersion: 0.1.0
description: A Helm chart for snow-white
keywords:
  - app
  - web
maintainers:
  - name: John Doe
    email: john@example.com
dependencies:
  - name: postgresql
    version: 12.0.0
`;

      vi.mocked(mockFs.readFileSync).mockReturnValue(complexChart);

      bumper.bumpChartVersion('3.0.0');

      const writtenContent = vi.mocked(mockFs.writeFileSync).mock.calls[0][1];

      // Check that appVersion was updated
      expect(writtenContent).toContain('version: 3.0.0');
      expect(writtenContent).toContain('appVersion: 3.0.0');

      // Check that other properties are preserved
      expect(writtenContent).toContain('name: snow-white');
      expect(writtenContent).toContain(
        'description: A Helm chart for snow-white',
      );
      expect(writtenContent).toContain('- app');
      expect(writtenContent).toContain('john@example.com');
      expect(writtenContent).toContain('postgresql');
    });
  });
});

describe('runCLI', () => {
  let consoleSpy: {
    error: ReturnType<typeof vi.spyOn>;
    log: ReturnType<typeof vi.spyOn>;
  };

  let processExitSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    consoleSpy = {
      error: vi.spyOn(console, 'error').mockImplementation(() => {}),
      log: vi.spyOn(console, 'log').mockImplementation(() => {}),
    };

    processExitSpy = vi.spyOn(process, 'exit').mockImplementation((code) => {
      throw new Error(`process.exit(${code})`);
    });
  });

  it('should show usage and exit when no version provided', () => {
    expect(() => runCLI(['node', 'script.js'])).toThrow('process.exit(1)');

    expect(consoleSpy.error).toHaveBeenCalledWith(
      'Usage: ts-node bump-chart-version.ts <version> <appVersion?>',
    );
    expect(consoleSpy.error).toHaveBeenCalledWith(
      'Example: ts-node bump-chart-version.ts 1.2.3',
    );
    expect(processExitSpy).toHaveBeenCalledWith(1);
  });

  it('should exit with code 2 when bumpChartVersion throws', () => {
    const chartVersionBumperMock = {
      bumpChartVersion: vi.fn().mockImplementation(() => {
        throw new Error('File not found');
      }),
    } as unknown as ChartVersionBumper;

    expect(() =>
      runCLI(
        ['node', 'script.js', 'invalid-path-that-causes-error'],
        chartVersionBumperMock,
      ),
    ).toThrow('process.exit(2)');

    expect(consoleSpy.error).toHaveBeenCalledWith(
      'Failed to bump chart version: File not found',
    );
    expect(processExitSpy).toHaveBeenCalledWith(2);
  });

  it('should call bumpChartVersion with correct version', () => {
    const chartVersionBumperMock = {
      bumpChartVersion: vi.fn(),
    } as unknown as ChartVersionBumper;

    const newVersion = '2.0.0';
    expect(() =>
      runCLI(['node', 'script.js', newVersion], chartVersionBumperMock),
    ).not.toThrow();

    expect(chartVersionBumperMock.bumpChartVersion).toHaveBeenCalledWith(
      newVersion,
    );
  });
});
