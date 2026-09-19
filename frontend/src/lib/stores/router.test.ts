import { describe, expect, it } from 'vitest';
import { parse } from './router.svelte';

describe('parse', () => {
  it('treats an empty hash as the movies page', () => {
    expect(parse('')).toEqual({ name: 'movies' });
    expect(parse('#')).toEqual({ name: 'movies' });
    expect(parse('#/')).toEqual({ name: 'movies' });
  });

  it('reads the movie pages', () => {
    expect(parse('#/movies')).toEqual({ name: 'movies' });
    expect(parse('#/movies/new')).toEqual({ name: 'movie-new' });
    expect(parse('#/movies/12')).toEqual({ name: 'movie-detail', id: '12' });
    expect(parse('#/movies/12/edit')).toEqual({ name: 'movie-edit', id: '12' });
  });

  it('reads the people pages', () => {
    expect(parse('#/people')).toEqual({ name: 'people' });
    expect(parse('#/people/new')).toEqual({ name: 'person-new' });
    expect(parse('#/people/3')).toEqual({ name: 'person-detail', id: '3' });
    expect(parse('#/people/3/edit')).toEqual({ name: 'person-edit', id: '3' });
  });

  it('reads the search text', () => {
    expect(parse('#/search?q=nolan')).toEqual({ name: 'search', query: 'nolan' });
  });

  it('decodes a search with spaces and symbols', () => {
    expect(parse('#/search?q=ready%20player%20one').query).toBe('ready player one');
    expect(parse('#/search?q=r%26b').query).toBe('r&b');
  });

  it('allows an empty search', () => {
    expect(parse('#/search?q=')).toEqual({ name: 'search', query: '' });
    expect(parse('#/search')).toEqual({ name: 'search', query: '' });
  });

  it('ignores extra slashes', () => {
    expect(parse('#//movies//12//')).toEqual({ name: 'movie-detail', id: '12' });
  });

  // ids are always numbers, so a typo should not be sent to the server
  it('refuses ids that are not numbers', () => {
    expect(parse('#/movies/abc')).toEqual({ name: 'not-found' });
    expect(parse('#/movies/1a')).toEqual({ name: 'not-found' });
    expect(parse('#/people/xyz')).toEqual({ name: 'not-found' });
  });

  // what happened when the hash was pasted onto an existing url
  it('refuses a hash inside a hash', () => {
    expect(parse('#/movies/#/nonsense')).toEqual({ name: 'not-found' });
  });

  it('returns not-found for unknown pages', () => {
    expect(parse('#/nonsense')).toEqual({ name: 'not-found' });
    expect(parse('#/movies-and-more')).toEqual({ name: 'not-found' });
  });
});
