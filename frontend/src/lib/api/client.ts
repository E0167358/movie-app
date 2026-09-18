// small GraphQL client. we don't use a library like Apollo because
// the app only needs "send a query, get data or an error back"

const GRAPHQL_URL = '/graphql';

export class GraphQlError extends Error {
  // "code" comes from extensions.code in the gateway, e.g. NOT_FOUND or ALREADY_EXISTS
  readonly code: string;

  constructor(message: string, code: string) {
    super(message);
    this.name = 'GraphQlError';
    this.code = code;
  }
}

interface GraphQlResponse<T> {
  data?: T | null;
  errors?: Array<{
    message: string;
    extensions?: { code?: string };
  }>;
}

export async function request<T>(
  query: string,
  variables: Record<string, unknown> = {},
): Promise<T> {
  let response: Response;

  try {
    response = await fetch(GRAPHQL_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query, variables }),
    });
  } catch {
    // the server is down or there is no network at all
    throw new GraphQlError('Cannot reach the server. Is the gateway running?', 'NETWORK');
  }

  if (!response.ok) {
    throw new GraphQlError(`Server returned ${response.status}`, 'HTTP_ERROR');
  }

  const body = (await response.json()) as GraphQlResponse<T>;

  // GraphQL can return data AND errors at the same time, for example when
  // the movie search worked but the people search failed
  if (body.errors && body.errors.length > 0) {
    const first = body.errors[0];
    throw new GraphQlError(first.message, first.extensions?.code ?? 'UNKNOWN');
  }

  if (!body.data) {
    throw new GraphQlError('Server returned no data', 'NO_DATA');
  }

  return body.data;
}
