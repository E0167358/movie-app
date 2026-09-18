import { request } from './client';
import type { Credit, CreditInput, Page, Person, PersonInput } from './types';

const PERSON_FIELDS = `
  id
  name
  bio
  birthYear
`;

export async function fetchPeople(page: number, size: number): Promise<Page<Person>> {
  const query = `
    query People($page: Int!, $size: Int!) {
      people(page: $page, size: $size) {
        totalCount
        items { ${PERSON_FIELDS} }
      }
    }
  `;
  const data = await request<{ people: Page<Person> }>(query, { page, size });
  return data.people;
}

export async function fetchPerson(id: string): Promise<Person | null> {
  const query = `
    query Person($id: ID!) {
      person(id: $id) {
        ${PERSON_FIELDS}
        credits {
          id
          role
          characterName
          movieId
          movie { id title releaseYear artworkUrl }
        }
      }
    }
  `;
  const data = await request<{ person: Person | null }>(query, { id });
  return data.person;
}

export async function createPerson(input: PersonInput): Promise<Person> {
  const query = `
    mutation CreatePerson($input: PersonInput!) {
      createPerson(input: $input) { ${PERSON_FIELDS} }
    }
  `;
  const data = await request<{ createPerson: Person }>(query, { input });
  return data.createPerson;
}

export async function updatePerson(id: string, input: PersonInput): Promise<Person> {
  const query = `
    mutation UpdatePerson($id: ID!, $input: PersonInput!) {
      updatePerson(id: $id, input: $input) { ${PERSON_FIELDS} }
    }
  `;
  const data = await request<{ updatePerson: Person }>(query, { id, input });
  return data.updatePerson;
}

export async function deletePerson(id: string): Promise<void> {
  const query = `
    mutation DeletePerson($id: ID!) {
      deletePerson(id: $id)
    }
  `;
  await request<{ deletePerson: boolean }>(query, { id });
}

// ----- credits -----

export async function addCredit(input: CreditInput): Promise<Credit> {
  const query = `
    mutation AddCredit($input: CreditInput!) {
      addCredit(input: $input) {
        id
        role
        characterName
        movieId
        person { ${PERSON_FIELDS} }
      }
    }
  `;
  const data = await request<{ addCredit: Credit }>(query, { input });
  return data.addCredit;
}

export async function removeCredit(id: string): Promise<void> {
  const query = `
    mutation RemoveCredit($id: ID!) {
      removeCredit(id: $id)
    }
  `;
  await request<{ removeCredit: boolean }>(query, { id });
}
