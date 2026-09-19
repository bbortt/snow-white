#!/usr/bin/env bash

#
# Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
# Licensed under the Polyform Small Business License 1.0.0
# See LICENSE file for full details.
#

# Runs PIT mutation testing (the `mutation` profile in the root pom.xml) scoped to only the
# classes changed on this branch, instead of the full reactor - the branches CI pipeline runs
# the unscoped, all-classes variant. Usage:
#   .github/scripts/pitest-changed-classes.sh [base-ref]
# base-ref defaults to 'main'. Committed changes since branching off base-ref are considered,
# plus anything still uncommitted or untracked locally.
#
# A changed src/test/java file only contributes a target when its own class-under-test can be
# derived by naming convention (Foo{UnitTest,Test} -> Foo); a test-only change that doesn't fit
# that convention (a fixture, a helper) won't scope in its subject and needs a manual
# -DtargetClasses run instead.

set -eu

base_ref="${1:-main}"

scripts_dir="$(dirname "$(realpath "$0")")"
root_dir="$scripts_dir/../.."
cd "$root_dir"

merge_base="$(git merge-base "$base_ref" HEAD)"

changed_files="$(
  {
    git diff --name-only --diff-filter=ACMR "$merge_base" -- '*.java'
    git ls-files --others --exclude-standard -- '*.java'
  } | sort -u
)"

if [[ -z "$changed_files" ]]; then
  echo "No changed .java files against $base_ref - nothing to mutation-test." 1>&2
  exit 0
fi

declare -A targets_by_module

to_fqcn() {
  echo "$1" | sed -E 's#^(.*/)?src/(main|test)/java/##; s#\.java$##; s#/#.#g'
}

module_of() {
  echo "$1" | sed -E 's#(/src/(main|test)/java/).*$##'
}

while IFS= read -r file; do
  [[ -z "$file" ]] && continue

  module="$(module_of "$file")"
  fqcn="$(to_fqcn "$file")"

  case "$file" in
    */src/main/java/*)
      class_name="$fqcn"
      ;;
    */src/test/java/*)
      base_fqcn="${fqcn%UnitTest}"
      base_fqcn="${base_fqcn%Test}"
      [[ "$base_fqcn" == "$fqcn" ]] && continue
      main_file="$module/src/main/java/$(echo "$base_fqcn" | sed 's#\.#/#g').java"
      [[ -f "$main_file" ]] || continue
      class_name="$base_fqcn"
      ;;
    *)
      continue
      ;;
  esac

  existing="${targets_by_module[$module]:-}"
  glob="$class_name,$class_name\$*"
  case ",$existing," in
    *",$glob,"*) ;;
    *) targets_by_module[$module]="${existing:+$existing,}$glob" ;;
  esac
done <<<"$changed_files"

if [[ ${#targets_by_module[@]} -eq 0 ]]; then
  echo "No changed .java files map to a mutable module - nothing to mutation-test." 1>&2
  exit 0
fi

for module in "${!targets_by_module[@]}"; do
  echo "Mutation testing changed classes in $module ..."
  ./mvnw -pl "$module" -am -P mutation verify -DtargetClasses="${targets_by_module[$module]}"
done
