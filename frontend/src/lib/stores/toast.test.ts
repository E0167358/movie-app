import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { toasts } from './toast.svelte';

describe('toast store', () => {
  beforeEach(() => {
    toasts.clear();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('starts empty', () => {
    expect(toasts.items).toEqual([]);
  });

  it('adds a success message', () => {
    toasts.success('Movie added');

    expect(toasts.items).toHaveLength(1);
    expect(toasts.items[0].kind).toBe('success');
    expect(toasts.items[0].message).toBe('Movie added');
  });

  it('adds an error message', () => {
    toasts.error('Could not save');

    expect(toasts.items[0].kind).toBe('error');
  });

  it('gives every message its own id', () => {
    toasts.success('one');
    toasts.success('two');

    const [first, second] = toasts.items;
    expect(first.id).not.toBe(second.id);
  });

  it('removes a success message after 4 seconds', () => {
    toasts.success('Movie added');

    vi.advanceTimersByTime(3999);
    expect(toasts.items).toHaveLength(1);

    vi.advanceTimersByTime(1);
    expect(toasts.items).toHaveLength(0);
  });

  // errors stay longer because the user may need to read them
  it('keeps an error message for 6 seconds', () => {
    toasts.error('Could not save');

    vi.advanceTimersByTime(4000);
    expect(toasts.items).toHaveLength(1);

    vi.advanceTimersByTime(2000);
    expect(toasts.items).toHaveLength(0);
  });

  it('dismisses only the message that was closed', () => {
    const first = toasts.success('one');
    toasts.success('two');

    toasts.dismiss(first);

    expect(toasts.items).toHaveLength(1);
    expect(toasts.items[0].message).toBe('two');
  });

  it('does nothing when dismissing an id that is already gone', () => {
    toasts.success('one');

    toasts.dismiss(999);

    expect(toasts.items).toHaveLength(1);
  });

  it('clears every message', () => {
    toasts.success('one');
    toasts.error('two');

    toasts.clear();

    expect(toasts.items).toEqual([]);
  });

  it('keeps a message with no duration on screen', () => {
    toasts.show('success', 'stays', 0);

    vi.advanceTimersByTime(60_000);

    expect(toasts.items).toHaveLength(1);
  });
});
