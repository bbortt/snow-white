/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

export const expectFailsWithMessageContaining = async (
  callback: Function,
  part: string,
): Promise<void> => {
  let errorMessage: string | undefined;
  try {
    await callback();
  } catch (error) {
    errorMessage = error.message;
    expect(error.message).contains(part);

    return;
  }

  console.error('Error message didn match:', errorMessage);

  expect.fail(`Expected code to throw exception containing message '${part}'!`);
};

export const isSubset = (
  subset: Record<string, string>,
  superset: Record<string, any>,
): boolean => {
  for (const key in subset) {
    if (!(key in superset) || subset[key] !== superset[key]) {
      return false;
    }
  }

  return true;
};
