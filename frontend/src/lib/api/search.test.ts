import { afterEach, describe, expect, it, vi } from 'vitest';
import { search } from './search';

function mockResponse(body: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => body }),
  );
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('search', () => {
  it('returns movies and people when both services answer', async () => {
    mockResponse({
      data: {
        search: {
          movies: { totalCount: 1, items: [{ id: '1', title: 'Inception' }] },
          people: { totalCount: 1, items: [{ id: '1', name: 'Christopher Nolan' }] },
        },
      },
    });

    const results = await search('nolan');

    expect(results.movies?.totalCount).toBe(1);
    expect(results.people?.items[0].name).toBe('Christopher Nolan');
    expect(results.movieError).toBeNull();
    expect(results.peopleError).toBeNull();
  });

  it('keeps the movie results when the people search failed', async () => {
    mockResponse({
      data: {
        search: {
          movies: { totalCount: 1, items: [{ id: '1', title: 'Inception' }] },
          people: null,
        },
      },
      errors: [
        {
          message: 'Service is not available right now, please try again',
          path: ['search', 'people'],
          extensions: { code: 'SERVICE_UNAVAILABLE' },
        },
      ],
    });

    const results = await search('nolan');

    expect(results.movies?.totalCount).toBe(1);
    expect(results.people).toBeNull();
    expect(results.peopleError).toContain('not available');
    expect(results.movieError).toBeNull();
  });

  it('keeps the people results when the movie search failed', async () => {
    mockResponse({
      data: {
        search: {
          movies: null,
          people: { totalCount: 2, items: [] },
        },
      },
      errors: [{ message: 'movie-service is down', path: ['search', 'movies'] }],
    });

    const results = await search('nolan');

    expect(results.people?.totalCount).toBe(2);
    expect(results.movieError).toBe('movie-service is down');
  });

  it('returns empty results when nothing matched', async () => {
    mockResponse({
      data: {
        search: {
          movies: { totalCount: 0, items: [] },
          people: { totalCount: 0, items: [] },
        },
      },
    });

    const results = await search('zzzz');

    expect(results.movies?.totalCount).toBe(0);
    expect(results.people?.totalCount).toBe(0);
    expect(results.movieError).toBeNull();
  });

  it('sends the query and the size', async () => {
    mockResponse({ data: { search: { movies: null, people: null } } });

    await search('nolan', 5);

    const fetchMock = vi.mocked(fetch);
    const body = JSON.parse(fetchMock.mock.calls[0][1]!.body as string);
    expect(body.variables).toEqual({ query: 'nolan', size: 5 });
  });
});
