/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';

export type TextWithCodeProps = {
  text: string | undefined;
};

type TextPart = { type: 'plaintext' | 'code'; contents: string; offset: number };

export const TextWithCode: React.FC<TextWithCodeProps> = ({ text }: TextWithCodeProps) => {
  if (!text || !text.includes('`') || (text.split('`').length - 1) % 2 !== 0) {
    return <span>{text}</span>;
  }

  const textParts: TextPart[] = [];
  let offset = 0;
  text.split('`').forEach((value, index) => {
    textParts.push({ type: index % 2 === 0 ? 'plaintext' : 'code', contents: value, offset });
    offset += value.length + 1; // +1 accounts for the consumed backtick delimiter
  });

  return (
    <>
      {textParts.map((textPart: TextPart) => {
        if (textPart.type === 'plaintext') {
          return <span key={textPart.offset}>{textPart.contents}</span>;
        } else if (textPart.type === 'code') {
          return <code key={textPart.offset}>{textPart.contents}</code>;
        }
      })}
    </>
  );
};
