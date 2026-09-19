import { afterEach, describe, expect, it, vi } from 'vitest';
import { GraphQlError, request, requestPartial } from './client';

function mockFetch(body: unknown, ok = true, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok,
    status,
    json: async () => body,
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('request', () => {
  it('sends the query and variables as JSON', async () => {
    const fetchMock = mockFetch({ data: { movies: { items: [], totalCount: 0 } } });

    await request('query Movies { movies { totalCount } }', { page: 0 });

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/graphql');
    expect(options.method).toBe('POST');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(options.body)).toEqual({
      query: 'query Movies { movies { totalCount } }',
      variables: { page: 0 },
    });
  });

  it('returns the data field', async () => {
    mockFetch({ data: { movie: { id: '1', title: 'Inception' } } });

    const data = await request<{ movie: { title: string } }>('query { movie { title } }');

    expect(data.movie.title).toBe('Inception');
  });

  it('throws a GraphQlError with the code from the gateway', async () => {
    mockFetch({
      errors: [{ message: 'Title is required', extensions: { code: 'INVALID_ARGUMENT' } }],
      data: null,
    });

    const error = await request('mutation { createMovie { id } }').catch((e) => e);

    expect(error).toBeInstanceOf(GraphQlError);
    expect(error.message).toBe('Title is required');
    expect(error.code).toBe('INVALID_ARGUMENT');
  });

  it('keeps the field path of the error', async () => {
    mockFetch({ errors: [{ message: 'boom', path: ['search', 'people'] }], data: null });

    const error = await request('query { search { people { totalCount } } }').catch((e) => e);

    expect(error.path).toEqual(['search', 'people']);
  });

  it('uses UNKNOWN when the server sends no code', async () => {
    mockFetch({ errors: [{ message: 'something' }], data: null });

    const error = await request('query { movies { totalCount } }').catch((e) => e);

    expect(error.code).toBe('UNKNOWN');
  });

  it('reports a friendly message when the server cannot be reached', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    const error = await request('query { movies { totalCount } }').catch((e) => e);

    expect(error.code).toBe('NETWORK');
    expect(error.message).toContain('Cannot reach the server');
  });

  it('reports the status when the response is not ok', async () => {
    mockFetch({}, false, 500);

    const error = await request('query { movies { totalCount } }').catch((e) => e);

    expect(error.code).toBe('HTTP_ERROR');
    expect(error.message).toContain('500');
  });

  it('throws when there is no data and no error', async () => {
    mockFetch({});

    const error = await request('query { movies { totalCount } }').catch((e) => e);

    expect(error.code).toBe('NO_DATA');
  });
});

describe('requestPartial', () => {
  it('returns data and errors together', async () => {
    // what the gateway sends when the movie search worked but people-service is down
    mockFetch({
      data: { search: { movies: { totalCount: 1, items: [] }, people: null } },
      errors: [
        {
          message: 'Service is not available right now, please try again',
          path: ['search', 'people'],
          extensions: { code: 'SERVICE_UNAVAILABLE' },
        },
      ],
    });

    const result = await requestPartial<{ search: { people: unknown } }>('query { search { movies { totalCount } } }');

    expect(result.data?.search.people).toBeNull();
    expect(result.errors).toHaveLength(1);
    expect(result.errors[0].path).toEqual(['search', 'people']);
  });

  it('returns an empty error list when everything worked', async () => {
    mockFetch({ data: { search: { movies: null, people: null } } });

    const result = await requestPartial('query { search { movies { totalCount } } }');

    expect(result.errors).toEqual([]);
  });

  it('still throws when the request itself failed', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    await expect(requestPartial('query { search { movies { totalCount } } }')).rejects.toThrow(
      GraphQlError,
    );
  });
});
