/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  endOfLine: 'lf',
  plugins: ['@prettier/plugin-xml', 'prettier-plugin-packagejson'],
  printWidth: 140,
  singleQuote: true,
  tabWidth: 2,
  useTabs: false,
  arrowParens: 'avoid',
  bracketSameLine: false,
  xmlWhitespaceSensitivity: 'ignore',
  overrides: [
    {
      files: '**/*.{ts,tsx}',
      options: {
        parser: 'typescript',
      },
    },
  ],
};
