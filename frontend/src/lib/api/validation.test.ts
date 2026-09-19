import { describe, expect, it } from 'vitest';
import {
  toOptionalNumber,
  toOptionalText,
  validateMovie,
  validatePerson,
  type MovieFormValues,
  type PersonFormValues,
} from './validation';

const thisYear = new Date().getFullYear();

function movieForm(overrides: Partial<MovieFormValues> = {}): MovieFormValues {
  return {
    title: 'Inception',
    description: '',
    releaseYear: '',
    genre: '',
    durationMinutes: '',
    ...overrides,
  };
}

function personForm(overrides: Partial<PersonFormValues> = {}): PersonFormValues {
  return { name: 'Christopher Nolan', bio: '', birthYear: '', ...overrides };
}

describe('validateMovie', () => {
  it('accepts a movie with only a title', () => {
    expect(validateMovie(movieForm())).toEqual({});
  });

  it('accepts a fully filled movie', () => {
    const errors = validateMovie(
      movieForm({
        description: 'A dream heist',
        releaseYear: '2010',
        genre: 'Sci-Fi',
        durationMinutes: '148',
      }),
    );
    expect(errors).toEqual({});
  });

  it('requires a title', () => {
    expect(validateMovie(movieForm({ title: '' })).title).toBe('Title is required');
  });

  it('treats a title of only spaces as empty', () => {
    expect(validateMovie(movieForm({ title: '   ' })).title).toBe('Title is required');
  });

  it('accepts a title of exactly 255 characters', () => {
    expect(validateMovie(movieForm({ title: 'a'.repeat(255) })).title).toBeUndefined();
  });

  it('rejects a title of 256 characters', () => {
    expect(validateMovie(movieForm({ title: 'a'.repeat(256) })).title).toBe(
      'Title must be 255 characters or less',
    );
  });

  it('rejects a genre longer than 100 characters', () => {
    expect(validateMovie(movieForm({ genre: 'g'.repeat(101) })).genre).toBeDefined();
  });

  it('accepts 1888, the year of the oldest film', () => {
    expect(validateMovie(movieForm({ releaseYear: '1888' })).releaseYear).toBeUndefined();
  });

  it('rejects a year before 1888', () => {
    expect(validateMovie(movieForm({ releaseYear: '1887' })).releaseYear).toContain('1888');
  });

  it('accepts a year five years from now', () => {
    const year = String(thisYear + 5);
    expect(validateMovie(movieForm({ releaseYear: year })).releaseYear).toBeUndefined();
  });

  it('rejects a year more than five years from now', () => {
    const year = String(thisYear + 6);
    expect(validateMovie(movieForm({ releaseYear: year })).releaseYear).toBeDefined();
  });

  it('rejects a year that is not a number', () => {
    expect(validateMovie(movieForm({ releaseYear: '20ab' })).releaseYear).toBeDefined();
  });

  it('rejects a year with decimals', () => {
    expect(validateMovie(movieForm({ releaseYear: '2010.5' })).releaseYear).toBeDefined();
  });

  it('rejects a duration of zero or less', () => {
    for (const duration of ['0', '-1']) {
      expect(validateMovie(movieForm({ durationMinutes: duration })).durationMinutes).toBe(
        'Duration must be a positive whole number',
      );
    }
  });

  it('rejects a duration that is not a number', () => {
    expect(validateMovie(movieForm({ durationMinutes: 'abc' })).durationMinutes).toBeDefined();
  });

  it('reports every problem at once', () => {
    const errors = validateMovie(
      movieForm({ title: '', releaseYear: '1700', durationMinutes: '-5' }),
    );
    expect(Object.keys(errors).sort()).toEqual(['durationMinutes', 'releaseYear', 'title']);
  });

  // this is the bug we hit in the browser: a number input binds a number,
  // and calling .trim() on it used to throw and hide every error message
  it('does not throw when a value arrives as a number instead of text', () => {
    const values = { ...movieForm(), releaseYear: 1800 as unknown as string };
    expect(() => validateMovie(values)).not.toThrow();
    expect(validateMovie(values).releaseYear).toBeDefined();
  });

  it('does not throw when a value is undefined', () => {
    const values = { ...movieForm(), genre: undefined as unknown as string };
    expect(() => validateMovie(values)).not.toThrow();
  });
});

describe('validatePerson', () => {
  it('accepts a person with only a name', () => {
    expect(validatePerson(personForm())).toEqual({});
  });

  it('requires a name', () => {
    expect(validatePerson(personForm({ name: '  ' })).name).toBe('Name is required');
  });

  it('rejects a name longer than 255 characters', () => {
    expect(validatePerson(personForm({ name: 'n'.repeat(256) })).name).toBeDefined();
  });

  it('accepts a bio of exactly 5000 characters', () => {
    expect(validatePerson(personForm({ bio: 'b'.repeat(5000) })).bio).toBeUndefined();
  });

  it('rejects a bio of 5001 characters', () => {
    expect(validatePerson(personForm({ bio: 'b'.repeat(5001) })).bio).toBeDefined();
  });

  it('accepts an old birth year, classic writers still get credits', () => {
    expect(validatePerson(personForm({ birthYear: '1564' })).birthYear).toBeUndefined();
  });

  it('accepts the current year', () => {
    expect(validatePerson(personForm({ birthYear: String(thisYear) })).birthYear).toBeUndefined();
  });

  it('rejects a birth year in the future', () => {
    expect(validatePerson(personForm({ birthYear: String(thisYear + 1) })).birthYear).toBeDefined();
  });

  it('rejects a birth year of zero or less', () => {
    expect(validatePerson(personForm({ birthYear: '0' })).birthYear).toBeDefined();
    expect(validatePerson(personForm({ birthYear: '-5' })).birthYear).toBeDefined();
  });
});

describe('form value helpers', () => {
  it('turns empty text into null and trims the rest', () => {
    expect(toOptionalText('')).toBeNull();
    expect(toOptionalText('   ')).toBeNull();
    expect(toOptionalText('  Sci-Fi ')).toBe('Sci-Fi');
  });

  it('turns empty numbers into null and parses the rest', () => {
    expect(toOptionalNumber('')).toBeNull();
    expect(toOptionalNumber('  ')).toBeNull();
    expect(toOptionalNumber(' 2010 ')).toBe(2010);
  });
});
