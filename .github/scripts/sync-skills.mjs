/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

/*
 * Mirrors APM-packaged skills from `.apm/skills/` into `.claude/skills/`.
 *
 * `.apm/skills/` is the source of truth and the only thing `apm pack` ships.
 * Claude Code reads `.claude/skills/`, so the copy is committed to keep the skill
 * working in this repo for contributors who do not have the APM CLI installed.
 *
 *   node .github/scripts/sync-skills.mjs           rewrite the copy
 *   node .github/scripts/sync-skills.mjs --check   fail if the copy has drifted
 *
 * Skills that exist only under `.claude/skills/` are left untouched -- they are not
 * published via APM.
 */

import { cpSync, mkdirSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { dirname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const rootDir = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const sourceDir = join(rootDir, '.apm', 'skills');
const targetDir = join(rootDir, '.claude', 'skills');

const check = process.argv.includes('--check');

function listFiles(dir) {
  const entries = readdirSync(dir, { recursive: true, withFileTypes: true });
  return entries
    .filter((entry) => entry.isFile())
    .map((entry) => relative(dir, join(entry.parentPath, entry.name)))
    .sort();
}

function diff(source, target) {
  let sourceFiles;
  let targetFiles;

  try {
    sourceFiles = listFiles(source);
    targetFiles = listFiles(target);
  } catch {
    return [`${relative(rootDir, target)} is missing`];
  }

  const problems = [];

  for (const file of sourceFiles) {
    if (!targetFiles.includes(file)) {
      problems.push(`missing: ${relative(rootDir, join(target, file))}`);
    } else if (
      !readFileSync(join(source, file)).equals(readFileSync(join(target, file)))
    ) {
      problems.push(`differs: ${relative(rootDir, join(target, file))}`);
    }
  }

  for (const file of targetFiles) {
    if (!sourceFiles.includes(file)) {
      problems.push(`unexpected: ${relative(rootDir, join(target, file))}`);
    }
  }

  return problems;
}

const skills = readdirSync(sourceDir, { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => entry.name)
  .sort();

if (skills.length === 0) {
  console.error(`No skills found in ${relative(rootDir, sourceDir)}`);
  process.exit(1);
}

const problems = [];

for (const skill of skills) {
  const source = join(sourceDir, skill);
  const target = join(targetDir, skill);

  if (check) {
    problems.push(...diff(source, target));
    continue;
  }

  rmSync(target, { force: true, recursive: true });
  mkdirSync(dirname(target), { recursive: true });
  cpSync(source, target, { recursive: true });
  console.log(`Synced ${skill} -> ${relative(rootDir, target)}`);
}

if (problems.length > 0) {
  console.error('Packaged skills are out of sync with .apm/skills/:\n');
  for (const problem of problems) {
    console.error(`  ${problem}`);
  }
  console.error('\nRun `pnpm run skill:sync` and commit the result.');
  process.exit(1);
}

if (check) {
  console.log(`Packaged skills are in sync (${skills.join(', ')}).`);
}
