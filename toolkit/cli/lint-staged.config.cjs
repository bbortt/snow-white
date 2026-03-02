/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  '{,src/**/}*.{cjs,js,mjs,ts,tsx}': ['eslint --fix', 'prettier --write --config prettier.config.js'],
};
