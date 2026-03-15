/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { render } from '@testing-library/react';
import hljs from 'highlight.js';
import React from 'react';

import { CodeHighlightBlock } from './code-highlight-block';

jest.mock('highlight.js', () => ({
  highlight: jest.fn(),
}));

jest.mock('./code-highlight-block.scss', () => ({}));
jest.mock('highlight.js/styles/default.css', () => ({}));

const mockHighlight = hljs.highlight as jest.MockedFunction<typeof hljs.highlight>;

describe('CodeHighlightBlock', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('without a language prop', () => {
    it('renders a <code> element', () => {
      const { container } = render(<CodeHighlightBlock code="hello world" />);

      expect(container.querySelector('code')).toBeInTheDocument();
    });

    it('renders the code as plain text', () => {
      const code = 'const x = 1;';
      const { container } = render(<CodeHighlightBlock code={code} />);

      const codeElement = container.querySelector('code');
      expect(codeElement).toBeInTheDocument();
      expect(codeElement).toHaveTextContent(code);
    });

    it('applies the css classes to the <code> element', () => {
      const { container } = render(<CodeHighlightBlock code="x" />);

      const code = container.querySelector('code');
      expect(code).toHaveClass('code-highlight-block');
      expect(code).toHaveClass('mt-2');
    });

    it('does not call hljs.highlight', () => {
      render(<CodeHighlightBlock code="anything" />);

      expect(mockHighlight).not.toHaveBeenCalled();
    });
  });

  describe('with a language prop', () => {
    it('calls hljs.highlight with the code and language', () => {
      mockHighlight.mockReturnValue({ value: '<span>highlighted</span>' } as any);

      render(<CodeHighlightBlock code="const x = 1;" language="javascript" />);

      expect(mockHighlight).toHaveBeenCalledTimes(1);
      expect(mockHighlight).toHaveBeenCalledWith('const x = 1;', { language: 'javascript' });
    });

    it('renders the highlighted HTML returned by hljs via dangerouslySetInnerHTML', () => {
      mockHighlight.mockReturnValue({ value: '<span class="hljs-keyword">const</span>' } as any);

      const { container } = render(<CodeHighlightBlock code="const x = 1;" language="javascript" />);

      const pre = container.querySelector('pre');
      expect(pre).toBeInTheDocument();
      expect(pre?.innerHTML).toBe('<span class="hljs-keyword">const</span>');
    });

    it('applies the css classes to the <pre> element', () => {
      mockHighlight.mockReturnValue({ value: '' } as any);

      const { container } = render(<CodeHighlightBlock code="" language="typescript" />);

      const pre = container.querySelector('pre');
      expect(pre).toHaveClass('code-highlight-block');
      expect(pre).toHaveClass('mt-2');
    });

    it('handles an empty string code value', () => {
      mockHighlight.mockReturnValue({ value: '' } as any);

      const { container } = render(<CodeHighlightBlock code="" language="json" />);

      expect(mockHighlight).toHaveBeenCalledWith('', { language: 'json' });
      expect(container.querySelector('pre')).toHaveTextContent('');
    });
  });
});
