<script lang="ts">
  import { fetchMovies } from '../api/movies';
  import { GraphQlError } from '../api/client';
  import type { Movie } from '../api/types';
  import MovieCard from '../components/MovieCard.svelte';
  import Spinner from '../components/Spinner.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';
  import Pagination from '../components/Pagination.svelte';

  const PAGE_SIZE = 12;

  let movies = $state<Movie[]>([]);
  let totalCount = $state(0);
  let page = $state(0);
  let loading = $state(true);
  let errorMessage = $state<string | null>(null);

  async function load() {
    loading = true;
    errorMessage = null;

    try {
      const result = await fetchMovies(page, PAGE_SIZE);
      movies = result.items;
      totalCount = result.totalCount;
    } catch (error) {
      errorMessage =
        error instanceof GraphQlError ? error.message : 'Could not load movies';
      movies = [];
    } finally {
      loading = false;
    }
  }

  function changePage(next: number) {
    page = next;
  }

  // runs on first render and again whenever the page number changes
  $effect(() => {
    // read page so the effect knows it depends on it
    page;
    load();
  });
</script>

<section>
  <header class="page-header">
    <div>
      <h2>Movies</h2>
      {#if !loading && !errorMessage}
        <p class="count">{totalCount} {totalCount === 1 ? 'movie' : 'movies'}</p>
      {/if}
    </div>
    <a class="btn btn-primary" href="#/movies/new">Add movie</a>
  </header>

  {#if loading}
    <Spinner label="Loading movies..." />
  {:else if errorMessage}
    <ErrorBox message={errorMessage} onRetry={load} />
  {:else if movies.length === 0}
    <div class="empty">
      <p>No movies yet.</p>
      <a class="btn btn-primary" href="#/movies/new">Add the first one</a>
    </div>
  {:else}
    <div class="grid">
      {#each movies as movie (movie.id)}
        <MovieCard {movie} />
      {/each}
    </div>

    <Pagination {page} size={PAGE_SIZE} {totalCount} onChange={changePage} />
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

  /* auto-fill means the number of columns comes from the screen width,
     so it works on phone and desktop without media queries */
  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 1rem;
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

  @media (max-width: 480px) {
    .grid {
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
    }
  }
</style>
