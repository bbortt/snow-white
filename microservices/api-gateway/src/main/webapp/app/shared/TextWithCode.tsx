/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React from 'react';

export type TextWithCodeProps = {
  text: string | undefined;
};

type TextPart = { type: 'plaintext' | 'code'; contents: string };

export const TextWithCode: React.FC<TextWithCodeProps> = ({ text }: TextWithCodeProps) => {
  if (!text || !text.includes('`') || (text.split('`').length - 1) % 2 !== 0) {
    return <span>{text}</span>;
  }

  const textParts: TextPart[] = [];
  text.split('`').forEach((value, index) => {
    textParts.push({ type: index % 2 === 0 ? 'plaintext' : 'code', contents: value });
  });

  return (
    <>
      {textParts.map((textPart: TextPart, index: number) => {
        if (textPart.type === 'plaintext') {
          return <span key={index}>{textPart.contents}</span>;
        } else if (textPart.type === 'code') {
          return <code key={index}>{textPart.contents}</code>;
        }
      })}
    </>
  );
};
