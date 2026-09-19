// small GraphQL client. we don't use a library like Apollo because
// the app only needs "send a query, get data or an error back"

const GRAPHQL_URL = '/graphql';

export class GraphQlError extends Error {
  // "code" comes from extensions.code in the gateway, e.g. NOT_FOUND or ALREADY_EXISTS
  readonly code: string;
  // which field failed, e.g. ["search", "people"]. used by the search page
  readonly path: string[];

  constructor(message: string, code: string, path: string[] = []) {
    super(message);
    this.name = 'GraphQlError';
    this.code = code;
    this.path = path;
  }
}

interface GraphQlResponse<T> {
  data?: T | null;
  errors?: Array<{
    message: string;
    path?: Array<string | number>;
    extensions?: { code?: string };
  }>;
}

export interface PartialResult<T> {
  data: T | null;
  errors: GraphQlError[];
}

async function send<T>(
  query: string,
  variables: Record<string, unknown>,
): Promise<GraphQlResponse<T>> {
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

  return (await response.json()) as GraphQlResponse<T>;
}

function toError(error: NonNullable<GraphQlResponse<unknown>['errors']>[number]): GraphQlError {
  return new GraphQlError(
    error.message,
    error.extensions?.code ?? 'UNKNOWN',
    (error.path ?? []).map(String),
  );
}

// normal calls: any error means the call failed
export async function request<T>(
  query: string,
  variables: Record<string, unknown> = {},
): Promise<T> {
  const body = await send<T>(query, variables);

  if (body.errors && body.errors.length > 0) {
    throw toError(body.errors[0]);
  }

  if (!body.data) {
    throw new GraphQlError('Server returned no data', 'NO_DATA');
  }

  return body.data;
}

// search calls: GraphQL can return data AND errors together, for example when
// the movie search worked but people-service is down. we keep both so the page
// can show the results it has
export async function requestPartial<T>(
  query: string,
  variables: Record<string, unknown> = {},
): Promise<PartialResult<T>> {
  const body = await send<T>(query, variables);

  return {
    data: body.data ?? null,
    errors: (body.errors ?? []).map(toError),
  };
}
