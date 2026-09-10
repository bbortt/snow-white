/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export default {
  '{,**/}*.{cjs,java,js,json,md,mjs,ts,xml,yaml,yml}': ['prettier --write'],
  '{,**/}*.md': ['markdownlint --rules markdownlint-sentences-per-line --fix'],
};
