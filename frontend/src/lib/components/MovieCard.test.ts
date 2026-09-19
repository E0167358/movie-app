import { render, screen } from '@testing-library/svelte';
import { describe, expect, it } from 'vitest';
import MovieCard from './MovieCard.svelte';
import type { Movie } from '../api/types';

function movie(overrides: Partial<Movie> = {}): Movie {
  return {
    id: '1',
    title: 'Inception',
    description: null,
    releaseYear: 2010,
    genre: 'Sci-Fi',
    durationMinutes: 148,
    artworkUrl: null,
    ...overrides,
  };
}

describe('MovieCard', () => {
  it('shows the title', () => {
    render(MovieCard, { props: { movie: movie() } });

    expect(screen.getByRole('heading', { name: 'Inception' })).toBeInTheDocument();
  });

  it('joins year, genre and duration into one line', () => {
    render(MovieCard, { props: { movie: movie() } });

    expect(screen.getByText('2010 · Sci-Fi · 148 min')).toBeInTheDocument();
  });

  it('leaves out the details it does not have', () => {
    render(MovieCard, { props: { movie: movie({ genre: null, durationMinutes: null }) } });

    expect(screen.getByText('2010')).toBeInTheDocument();
  });

  it('shows a message when there are no details at all', () => {
    const empty = movie({ releaseYear: null, genre: null, durationMinutes: null });
    render(MovieCard, { props: { movie: empty } });

    expect(screen.getByText('No details yet')).toBeInTheDocument();
  });

  it('links to the movie page', () => {
    render(MovieCard, { props: { movie: movie({ id: '42' }) } });

    expect(screen.getByRole('link')).toHaveAttribute('href', '#/movies/42');
  });

  it('shows a placeholder when there is no artwork', () => {
    render(MovieCard, { props: { movie: movie() } });

    expect(screen.getByText('No artwork')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows the artwork when there is one', () => {
    render(MovieCard, { props: { movie: movie({ artworkUrl: '/artwork/1' }) } });

    const image = screen.getByRole('img');
    expect(image).toHaveAttribute('src', '/artwork/1');
    expect(image).toHaveAccessibleName('Artwork for Inception');
  });
});
