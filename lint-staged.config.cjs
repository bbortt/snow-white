/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

module.exports = {
  '{,**/}*.{java,js,json,md,ts,xml,yaml,yml}': ['prettier --write'],
    '{,**/}*.md': ['markdownlint --rules markdownlint-sentences-per-line --fix']
};
