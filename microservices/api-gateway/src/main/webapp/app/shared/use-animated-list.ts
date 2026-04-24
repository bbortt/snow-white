/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { CSS_TRANSITION_TIMEOUT } from 'app/config/constants';
import { useEffect, useRef, useState } from 'react';

const STAGGER_DELAY = 30;

export function useAnimatedList<T>(list: T[], getKey: (item: T) => string): { displayedList: T[]; isExiting: boolean } {
  const [displayedList, setDisplayedList] = useState<T[]>(list);
  const [isExiting, setIsExiting] = useState(false);
  const isExitingRef = useRef(false);
  const latestListRef = useRef(list);
  const getKeyRef = useRef(getKey);
  latestListRef.current = list;
  getKeyRef.current = getKey;

  useEffect(() => {
    if (isExitingRef.current || list.length === 0) return;

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

  return { displayedList, isExiting };
}
