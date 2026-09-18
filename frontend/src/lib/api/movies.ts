import { request } from './client';
import type { Movie, MovieInput, Page } from './types';

const MOVIE_FIELDS = `
  id
  title
  description
  releaseYear
  genre
  durationMinutes
  artworkUrl
`;

export async function fetchMovies(page: number, size: number): Promise<Page<Movie>> {
  const query = `
    query Movies($page: Int!, $size: Int!) {
      movies(page: $page, size: $size) {
        totalCount
        items { ${MOVIE_FIELDS} }
      }
    }
  `;
  const data = await request<{ movies: Page<Movie> }>(query, { page, size });
  return data.movies;
}

export async function fetchMovie(id: string): Promise<Movie | null> {
  const query = `
    query Movie($id: ID!) {
      movie(id: $id) {
        ${MOVIE_FIELDS}
        credits {
          id
          role
          characterName
          person { id name birthYear }
        }
      }
    }
  `;
  const data = await request<{ movie: Movie | null }>(query, { id });
  return data.movie;
}

export async function createMovie(input: MovieInput): Promise<Movie> {
  const query = `
    mutation CreateMovie($input: MovieInput!) {
      createMovie(input: $input) { ${MOVIE_FIELDS} }
    }
  `;
  const data = await request<{ createMovie: Movie }>(query, { input });
  return data.createMovie;
}

export async function updateMovie(id: string, input: MovieInput): Promise<Movie> {
  const query = `
    mutation UpdateMovie($id: ID!, $input: MovieInput!) {
      updateMovie(id: $id, input: $input) { ${MOVIE_FIELDS} }
    }
  `;
  const data = await request<{ updateMovie: Movie }>(query, { id, input });
  return data.updateMovie;
}

export async function deleteMovie(id: string): Promise<void> {
  const query = `
    mutation DeleteMovie($id: ID!) {
      deleteMovie(id: $id)
    }
  `;
  await request<{ deleteMovie: boolean }>(query, { id });
}
