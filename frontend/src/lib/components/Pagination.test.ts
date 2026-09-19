import { render, screen } from '@testing-library/svelte';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import Pagination from './Pagination.svelte';

describe('Pagination', () => {
  it('shows nothing when everything fits on one page', () => {
    render(Pagination, {
      props: { page: 0, size: 12, totalCount: 12, onChange: vi.fn() },
    });

    expect(screen.queryByRole('navigation')).not.toBeInTheDocument();
  });

  it('appears when there is more than one page', () => {
    render(Pagination, {
      props: { page: 0, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByRole('navigation', { name: 'Pagination' })).toBeInTheDocument();
  });

  it('shows which items are on screen', () => {
    render(Pagination, {
      props: { page: 0, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByText('1-12 of 30')).toBeInTheDocument();
  });

  it('counts from the right place on a later page', () => {
    render(Pagination, {
      props: { page: 1, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByText('13-24 of 30')).toBeInTheDocument();
  });

  it('does not count past the last item', () => {
    render(Pagination, {
      props: { page: 2, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByText('25-30 of 30')).toBeInTheDocument();
  });

  it('disables Previous on the first page', () => {
    render(Pagination, {
      props: { page: 0, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByRole('button', { name: 'Previous' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled();
  });

  it('disables Next on the last page', () => {
    render(Pagination, {
      props: { page: 2, size: 12, totalCount: 30, onChange: vi.fn() },
    });

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Previous' })).toBeEnabled();
  });

  it('asks for the next page when Next is clicked', async () => {
    const onChange = vi.fn();
    render(Pagination, { props: { page: 1, size: 12, totalCount: 30, onChange } });

    await userEvent.click(screen.getByRole('button', { name: 'Next' }));

    expect(onChange).toHaveBeenCalledWith(2);
  });

  it('asks for the previous page when Previous is clicked', async () => {
    const onChange = vi.fn();
    render(Pagination, { props: { page: 1, size: 12, totalCount: 30, onChange } });

    await userEvent.click(screen.getByRole('button', { name: 'Previous' }));

    expect(onChange).toHaveBeenCalledWith(0);
  });
});
