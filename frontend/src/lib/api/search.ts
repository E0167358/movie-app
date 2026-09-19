import { requestPartial } from './client';
import type { Movie, Page, Person } from './types';

export interface SearchResults {
  movies: Page<Movie> | null;
  people: Page<Person> | null;
  // set when one of the two services failed, the other side still shows results
  movieError: string | null;
  peopleError: string | null;
}

interface SearchData {
  search: {
    movies: Page<Movie> | null;
    people: Page<Person> | null;
  } | null;
}

export async function search(query: string, size = 10): Promise<SearchResults> {
  const graphql = `
    query Search($query: String!, $size: Int!) {
      search(query: $query, size: $size) {
        movies {
          totalCount
          items { id title releaseYear genre durationMinutes artworkUrl }
        }
        people {
          totalCount
          items { id name birthYear bio }
        }
      }
    }
  `;

  const { data, errors } = await requestPartial<SearchData>(graphql, { query, size });

  // the gateway runs both searches at the same time, so an error on one field
  // does not stop the other. errors carry the field that failed in "path"
  const findError = (field: string) =>
    errors.find((error) => error.path.includes(field))?.message ?? null;

  return {
    movies: data?.search?.movies ?? null,
    people: data?.search?.people ?? null,
    movieError: data?.search?.movies ? null : findError('movies'),
    peopleError: data?.search?.people ? null : findError('people'),
  };
}
