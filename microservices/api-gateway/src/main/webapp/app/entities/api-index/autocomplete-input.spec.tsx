/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React, { useState } from 'react';

import { AutocompleteInput } from './autocomplete-input';

const SUGGESTIONS = ['apple', 'banana', 'apricot', 'cherry'];

const Controlled = ({ initialValue = '', suggestions = SUGGESTIONS }: { initialValue?: string; suggestions?: string[] }) => {
  const [value, setValue] = useState(initialValue);
  return <AutocompleteInput value={value} onChange={setValue} suggestions={suggestions} placeholder="Search..." />;
};

describe('AutocompleteInput', () => {
  describe('rendering', () => {
    it('renders the input with the given placeholder', () => {
      render(<AutocompleteInput value="" onChange={jest.fn()} suggestions={[]} placeholder="Type here" />);
      expect(screen.getByPlaceholderText('Type here')).toBeInTheDocument();
    });

    it('does not show the dropdown on initial render', () => {
      render(<Controlled />);
      expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });

    it('applies an extra className to the wrapper element', () => {
      render(<AutocompleteInput value="" onChange={jest.fn()} suggestions={[]} className="mt-1" />);
      expect(screen.getByRole('textbox').closest('div')).toHaveClass('mt-1');
    });
  });

  describe('dropdown visibility', () => {
    it('shows all suggestions on focus', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      expect(screen.getByText('apple')).toBeInTheDocument();
      expect(screen.getByText('banana')).toBeInTheDocument();
      expect(screen.getByText('apricot')).toBeInTheDocument();
      expect(screen.getByText('cherry')).toBeInTheDocument();
    });

    it('does not render the dropdown when no suggestions match', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.type(screen.getByRole('textbox'), 'xyz');
      expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });

    it('closes the dropdown on Escape', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      expect(screen.getAllByRole('listitem')).toHaveLength(4);
      await user.keyboard('{Escape}');
      expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });

    it('closes the dropdown when clicking outside the component', async () => {
      const user = userEvent.setup();
      render(
        <div>
          <Controlled />
          <button>Outside</button>
        </div>,
      );
      await user.click(screen.getByRole('textbox'));
      expect(screen.getAllByRole('listitem').length).toBeGreaterThan(0);
      fireEvent.mouseDown(screen.getByRole('button', { name: 'Outside' }));
      expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });
  });

  describe('suggestion filtering', () => {
    it('filters suggestions by the current input value', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.type(screen.getByRole('textbox'), 'ap');
      expect(screen.getByText('apple')).toBeInTheDocument();
      expect(screen.getByText('apricot')).toBeInTheDocument();
      expect(screen.queryByText('banana')).not.toBeInTheDocument();
      expect(screen.queryByText('cherry')).not.toBeInTheDocument();
    });

    it('filters case-insensitively', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.type(screen.getByRole('textbox'), 'AP');
      expect(screen.getByText('apple')).toBeInTheDocument();
      expect(screen.getByText('apricot')).toBeInTheDocument();
    });

    it('excludes the suggestion that exactly matches the current input', async () => {
      const user = userEvent.setup();
      render(<Controlled suggestions={['app', 'apple', 'application']} />);
      await user.type(screen.getByRole('textbox'), 'app');
      expect(screen.queryByText('app')).not.toBeInTheDocument();
      expect(screen.getByText('apple')).toBeInTheDocument();
      expect(screen.getByText('application')).toBeInTheDocument();
    });
  });

  describe('suggestion selection', () => {
    it('calls onChange with the selected suggestion on mousedown', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      fireEvent.mouseDown(screen.getByText('banana'));
      expect(screen.getByRole('textbox')).toHaveValue('banana');
    });

    it('closes the dropdown after a suggestion is selected', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      fireEvent.mouseDown(screen.getByText('cherry'));
      expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
    });
  });

  describe('keyboard navigation', () => {
    it('highlights the first suggestion on ArrowDown', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowDown}');
      const items = screen.getAllByRole('listitem');
      expect(items[0]).toHaveClass('active');
      expect(items[1]).not.toHaveClass('active');
    });

    it('moves the highlight down on repeated ArrowDown presses', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowDown}{ArrowDown}');
      const items = screen.getAllByRole('listitem');
      expect(items[0]).not.toHaveClass('active');
      expect(items[1]).toHaveClass('active');
    });

    it('moves the highlight back up on ArrowUp', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowDown}{ArrowDown}{ArrowUp}');
      const items = screen.getAllByRole('listitem');
      expect(items[0]).toHaveClass('active');
      expect(items[1]).not.toHaveClass('active');
    });

    it('clamps at -1 (nothing highlighted) when pressing ArrowUp with no active item', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowUp}{ArrowUp}');
      screen.getAllByRole('listitem').forEach(item => expect(item).not.toHaveClass('active'));
    });

    it('clamps at the last suggestion when pressing ArrowDown past the end', async () => {
      const user = userEvent.setup();
      render(<Controlled suggestions={['a', 'b']} />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowDown}{ArrowDown}{ArrowDown}{ArrowDown}');
      const items = screen.getAllByRole('listitem');
      expect(items[1]).toHaveClass('active');
    });

    it('selects the highlighted suggestion on Enter', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{ArrowDown}{Enter}');
      expect(screen.getByRole('textbox')).toHaveValue('apple');
    });

    it('does not select when Enter is pressed with no highlighted suggestion', async () => {
      const user = userEvent.setup();
      render(<Controlled />);
      await user.click(screen.getByRole('textbox'));
      await user.keyboard('{Enter}');
      expect(screen.getByRole('textbox')).toHaveValue('');
    });
  });
});
