/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import React, { useEffect, useRef, useState } from 'react';
import { Input } from 'reactstrap';

import './autocomplete-input.scss';

export type AutocompleteInputProps = {
  value: string;
  onChange: (value: string) => void;
  suggestions: string[];
  placeholder?: string;
  bsSize?: 'sm' | 'lg';
  className?: string;
};

export const AutocompleteInput: React.FC<AutocompleteInputProps> = ({ value, onChange, suggestions, placeholder, bsSize, className }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const containerRef = useRef<HTMLDivElement>(null);

  const filtered = suggestions.filter(s => s.toLowerCase().startsWith(value.toLowerCase()) && s !== value);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value);
    setIsOpen(true);
    setActiveIndex(-1);
  };

  const handleSelect = (suggestion: string) => {
    onChange(suggestion);
    setIsOpen(false);
    setActiveIndex(-1);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen || filtered.length === 0) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex(i => Math.min(i + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex(i => Math.max(i - 1, -1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      handleSelect(filtered[activeIndex]);
    } else if (e.key === 'Escape') {
      setIsOpen(false);
    }
  };

  const wrapperClassName = ['autocomplete-input-wrapper', className].filter(Boolean).join(' ');

  return (
    <div ref={containerRef} className={wrapperClassName}>
      <Input
        type="text"
        bsSize={bsSize}
        value={value}
        onChange={handleChange}
        onFocus={() => setIsOpen(true)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        autoComplete="off"
        role="combobox"
        aria-expanded={isOpen && filtered.length > 0}
        aria-autocomplete="list"
      />
      {isOpen && filtered.length > 0 && (
        <ul className="autocomplete-dropdown list-group" role="listbox">
          {filtered.map((s, i) => {
            const itemClassName = ['list-group-item', 'list-group-item-action', i === activeIndex ? 'active' : '']
              .filter(Boolean)
              .join(' ');
            return (
              <li key={s} className={itemClassName} role="option" aria-selected={i === activeIndex} onMouseDown={() => handleSelect(s)}>
                {s}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};
