/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import hljs from 'highlight.js';
import 'highlight.js/styles/default.css';
import React from 'react';

import './code-highlight-block.scss';

export interface ICodeHighlightBlockProps {
  code: string;
  language?: string;
}

export const CodeHighlightBlock = ({ code, language }: ICodeHighlightBlockProps) => {
  if (language) {
    return <pre className="mt-2 code-highlight-block" dangerouslySetInnerHTML={{ __html: hljs.highlight(code, { language }).value }} />;
  }

  return <code className="mt-2 code-highlight-block">{code}</code>;
};
