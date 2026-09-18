<script lang="ts">
  import { fetchPeople } from '../api/people';
  import { GraphQlError } from '../api/client';
  import type { Person } from '../api/types';
  import Spinner from '../components/Spinner.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';
  import Pagination from '../components/Pagination.svelte';

  const PAGE_SIZE = 20;

  let people = $state<Person[]>([]);
  let totalCount = $state(0);
  let page = $state(0);
  let loading = $state(true);
  let errorMessage = $state<string | null>(null);

  async function load() {
    loading = true;
    errorMessage = null;

    try {
      const result = await fetchPeople(page, PAGE_SIZE);
      people = result.items;
      totalCount = result.totalCount;
    } catch (error) {
      errorMessage = error instanceof GraphQlError ? error.message : 'Could not load people';
      people = [];
    } finally {
      loading = false;
    }
  }

  $effect(() => {
    page;
    load();
  });
</script>

<section>
  <header class="page-header">
    <div>
      <h2>Cast &amp; Creators</h2>
      {#if !loading && !errorMessage}
        <p class="count">{totalCount} {totalCount === 1 ? 'person' : 'people'}</p>
      {/if}
    </div>
    <a class="btn btn-primary" href="#/people/new">Add person</a>
  </header>

  {#if loading}
    <Spinner label="Loading people..." />
  {:else if errorMessage}
    <ErrorBox message={errorMessage} onRetry={load} />
  {:else if people.length === 0}
    <div class="empty">
      <p>No people yet.</p>
      <a class="btn btn-primary" href="#/people/new">Add the first one</a>
    </div>
  {:else}
    <ul class="people">
      {#each people as person (person.id)}
        <li>
          <a href="#/people/{person.id}">
            <span class="name">{person.name}</span>
            {#if person.birthYear}
              <span class="year">Born {person.birthYear}</span>
            {/if}
            {#if person.bio}
              <span class="bio">{person.bio}</span>
            {/if}
          </a>
        </li>
      {/each}
    </ul>

    <Pagination {page} size={PAGE_SIZE} {totalCount} onChange={(next) => (page = next)} />
  {/if}
</section>

<style>
  .page-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    flex-wrap: wrap;
    margin-bottom: 1.5rem;
  }

  h2 {
    margin: 0;
  }

  .count {
    margin: 0.25rem 0 0;
    color: var(--text-muted);
    font-size: 0.9rem;
  }

  .people {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 0.75rem;
  }

  .people a {
    display: flex;
    flex-direction: column;
    gap: 0.15rem;
    padding: 0.85rem 1rem;
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 10px;
    text-decoration: none;
    color: inherit;
  }

  .people a:hover,
  .people a:focus-visible {
    border-color: var(--accent);
  }

  .name {
    font-weight: 600;
  }

  .year {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  /* keep long bios to one line so every card is the same height */
  .bio {
    font-size: 0.85rem;
    color: var(--text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    padding: 3rem 1rem;
    color: var(--text-muted);
  }

  .empty p {
    margin: 0;
  }
</style>
