/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export default {
  endOfLine: 'lf',
  plugins: [
    '@prettier/plugin-xml',
    'prettier-plugin-java',
    'prettier-plugin-packagejson',
  ],
  printWidth: 80,
  singleQuote: true,
  tabWidth: 2,
  useTabs: false,
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
