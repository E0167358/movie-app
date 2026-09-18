// these match the GraphQL schema in gateway-service

export type Role = 'ACTOR' | 'DIRECTOR' | 'WRITER' | 'PRODUCER';

export const ROLES: Role[] = ['ACTOR', 'DIRECTOR', 'WRITER', 'PRODUCER'];

export interface Movie {
  id: string;
  title: string;
  description: string | null;
  releaseYear: number | null;
  genre: string | null;
  durationMinutes: number | null;
  artworkUrl: string | null;
  credits?: Credit[];
}

export interface Person {
  id: string;
  name: string;
  bio: string | null;
  birthYear: number | null;
  credits?: Credit[];
}

export interface Credit {
  id: string;
  role: Role;
  characterName: string | null;
  movieId: string;
  person: Person;
  movie?: Movie | null;
}

export interface Page<T> {
  items: T[];
  totalCount: number;
}

export interface MovieInput {
  title: string;
  description?: string | null;
  releaseYear?: number | null;
  genre?: string | null;
  durationMinutes?: number | null;
}

export interface PersonInput {
  name: string;
  bio?: string | null;
  birthYear?: number | null;
}

export interface CreditInput {
  movieId: string;
  personId: string;
  role: Role;
  characterName?: string | null;
}
