// the same rules the services use. we check them here too so the user
// sees the problem before a request is sent

export const FIRST_MOVIE_YEAR = 1888;

export type Errors = Record<string, string>;

export interface MovieFormValues {
  title: string;
  description: string;
  releaseYear: string;
  genre: string;
  durationMinutes: string;
}

export interface PersonFormValues {
  name: string;
  bio: string;
  birthYear: string;
}

// form values come from inputs, and a number input can bind a number or
// undefined instead of a string, so everything is normalised to text first
function asText(value: unknown): string {
  return value === null || value === undefined ? '' : String(value);
}

function checkYear(value: string, min: number, max: number, label: string): string | null {
  if (value === '') return null;

  const year = Number(value);
  if (!Number.isInteger(year)) {
    return `${label} must be a whole number`;
  }
  if (year < min || year > max) {
    return `${label} must be between ${min} and ${max}`;
  }
  return null;
}

export function validateMovie(values: MovieFormValues): Errors {
  const errors: Errors = {};
  const maxYear = new Date().getFullYear() + 5;

  const title = asText(values.title).trim();
  const genre = asText(values.genre).trim();
  const releaseYear = asText(values.releaseYear).trim();
  const duration = asText(values.durationMinutes).trim();

  if (title === '') {
    errors.title = 'Title is required';
  } else if (title.length > 255) {
    errors.title = 'Title must be 255 characters or less';
  }

  if (genre.length > 100) {
    errors.genre = 'Genre must be 100 characters or less';
  }

  const yearError = checkYear(releaseYear, FIRST_MOVIE_YEAR, maxYear, 'Release year');
  if (yearError) {
    errors.releaseYear = yearError;
  }

  if (duration !== '') {
    const minutes = Number(duration);
    if (!Number.isInteger(minutes) || minutes <= 0) {
      errors.durationMinutes = 'Duration must be a positive whole number';
    }
  }

  return errors;
}

export function validatePerson(values: PersonFormValues): Errors {
  const errors: Errors = {};

  const name = asText(values.name).trim();
  const bio = asText(values.bio).trim();
  const birthYear = asText(values.birthYear).trim();

  if (name === '') {
    errors.name = 'Name is required';
  } else if (name.length > 255) {
    errors.name = 'Name must be 255 characters or less';
  }

  if (bio.length > 5000) {
    errors.bio = 'Bio must be 5000 characters or less';
  }

  const yearError = checkYear(birthYear, 1, new Date().getFullYear(), 'Birth year');
  if (yearError) {
    errors.birthYear = yearError;
  }

  return errors;
}

// empty text fields are sent as null, and numbers as numbers
export function toOptionalText(value: string): string | null {
  const trimmed = asText(value).trim();
  return trimmed === '' ? null : trimmed;
}

export function toOptionalNumber(value: string): number | null {
  const trimmed = asText(value).trim();
  return trimmed === '' ? null : Number(trimmed);
}
