/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  arrowParens: 'avoid',
  bracketSameLine: false,
  endOfLine: 'lf',
  overrides: [
    {
      files: '**/*.{ts,tsx}',
      options: {
        parser: 'typescript',
      },
    },
  ],
  plugins: ['@prettier/plugin-xml', 'prettier-plugin-packagejson'],
  printWidth: 140,
  singleQuote: true,
  tabWidth: 2,
  useTabs: false,
  xmlWhitespaceSensitivity: 'ignore',
};
