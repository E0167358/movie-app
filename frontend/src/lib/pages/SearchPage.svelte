<script lang="ts">
  import { search, type SearchResults } from '../api/search';
  import { GraphQlError } from '../api/client';
  import MovieCard from '../components/MovieCard.svelte';
  import Spinner from '../components/Spinner.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';

  let { query }: { query: string } = $props();

  let results = $state<SearchResults | null>(null);
  let loading = $state(false);
  let errorMessage = $state<string | null>(null);

  let movieCount = $derived(results?.movies?.totalCount ?? 0);
  let peopleCount = $derived(results?.people?.totalCount ?? 0);
  let nothingFound = $derived(
    results !== null && movieCount === 0 && peopleCount === 0 &&
      !results.movieError && !results.peopleError,
  );

  async function run() {
    if (query.trim() === '') {
      results = null;
      return;
    }

    loading = true;
    errorMessage = null;

    try {
      results = await search(query.trim());
    } catch (error) {
      errorMessage = error instanceof GraphQlError ? error.message : 'Search failed';
      results = null;
    } finally {
      loading = false;
    }
  }

  // runs again whenever the search text in the url changes
  $effect(() => {
    query;
    run();
  });
</script>

<section>
  <h2>
    {#if query.trim()}
      Results for &ldquo;{query.trim()}&rdquo;
    {:else}
      Search
    {/if}
  </h2>

  {#if query.trim() === ''}
    <p class="muted">Type in the search box above to find movies, cast and creators.</p>
  {:else if loading}
    <Spinner label="Searching..." />
  {:else if errorMessage}
    <ErrorBox message={errorMessage} onRetry={run} />
  {:else if nothingFound}
    <p class="muted">Nothing matched your search.</p>
  {:else if results}
    <div class="group">
      <h3>Movies {#if movieCount}<span class="count">{movieCount}</span>{/if}</h3>

      {#if results.movieError}
        <!-- one service can fail while the other still answers -->
        <p class="partial">{results.movieError}</p>
      {:else if movieCount === 0}
        <p class="muted">No movies matched.</p>
      {:else}
        <div class="grid">
          {#each results.movies!.items as movie (movie.id)}
            <MovieCard {movie} />
          {/each}
        </div>
      {/if}
    </div>

    <div class="group">
      <h3>Cast &amp; Creators {#if peopleCount}<span class="count">{peopleCount}</span>{/if}</h3>

      {#if results.peopleError}
        <p class="partial">{results.peopleError}</p>
      {:else if peopleCount === 0}
        <p class="muted">No people matched.</p>
      {:else}
        <ul class="people">
          {#each results.people!.items as person (person.id)}
            <li>
              <a href="#/people/{person.id}">
                <span class="name">{person.name}</span>
                {#if person.birthYear}
                  <span class="year">Born {person.birthYear}</span>
                {/if}
              </a>
            </li>
          {/each}
        </ul>
      {/if}
    </div>
  {/if}
</section>

<style>
  h2 {
    margin: 0 0 1.5rem;
  }

  .group {
    margin-bottom: 2.5rem;
  }

  h3 {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin: 0 0 0.75rem;
    font-size: 1rem;
  }

  .count {
    font-size: 0.8rem;
    font-weight: 400;
    color: var(--text-muted);
    background: var(--surface-alt);
    border-radius: 999px;
    padding: 0.1rem 0.5rem;
  }

  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 1rem;
  }

  .people {
    list-style: none;
    margin: 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
    gap: 0.75rem;
  }

  .people a {
    display: flex;
    flex-direction: column;
    padding: 0.75rem 1rem;
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

  .muted {
    color: var(--text-muted);
  }

  .partial {
    color: var(--danger);
    font-size: 0.9rem;
  }

  @media (max-width: 480px) {
    .grid {
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    }
  }
</style>
