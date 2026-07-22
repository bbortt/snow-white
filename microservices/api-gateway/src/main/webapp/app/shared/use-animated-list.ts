/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useEffect, useRef, useState } from 'react';

const STAGGER_DELAY = 30;

export function useAnimatedList<T>(
  list: T[],
  getKey: (item: T) => string,
): { displayedList: T[]; isExiting: boolean; animationsEnabled: boolean } {
  const [displayedList, setDisplayedList] = useState<T[]>(list);
  const [isExiting, setIsExiting] = useState(false);
  const isExitingRef = useRef(false);
  const latestListRef = useRef(list);
  const getKeyRef = useRef(getKey);
  latestListRef.current = list;
  getKeyRef.current = getKey;

  // Skips the entrance transition for the very first batch of rows shown after mount/navigation
  // (initial fetch resolving into an empty table), so pages don't render half-faded-in on load.
  // Flips on once content has been shown, so later genuine updates (refresh/filter/sort) still animate.
  const [animationsEnabled, setAnimationsEnabled] = useState(false);
  useEffect(() => {
    if (!animationsEnabled && displayedList.length > 0) {
      setAnimationsEnabled(true);
    }
  }, [animationsEnabled, displayedList]);

  useEffect(() => {
    if (isExitingRef.current) return;

    const getK = getKeyRef.current;
    const prevKeys = displayedList.map(getK).join(',');
    const newKeys = list.map(getK).join(',');

    if (prevKeys === newKeys) return;

    if (displayedList.length === 0) {
      setDisplayedList(list);
      return;
    }

    isExitingRef.current = true;
    setIsExiting(true);
    const duration = CSS_TRANSITION_TIMEOUT + Math.max(0, displayedList.length - 1) * STAGGER_DELAY;

    const timer = setTimeout(() => {
      isExitingRef.current = false;
      setDisplayedList(latestListRef.current);
      setIsExiting(false);
    }, duration);

    return () => clearTimeout(timer);
  }, [list, displayedList]);

  return { displayedList, isExiting, animationsEnabled };
}
