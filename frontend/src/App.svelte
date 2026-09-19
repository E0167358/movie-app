<script lang="ts">
  import { router } from './lib/stores/router.svelte';
  import MoviesPage from './lib/pages/MoviesPage.svelte';
  import MovieDetailPage from './lib/pages/MovieDetailPage.svelte';
  import MovieFormPage from './lib/pages/MovieFormPage.svelte';
  import PeoplePage from './lib/pages/PeoplePage.svelte';
  import PersonDetailPage from './lib/pages/PersonDetailPage.svelte';
  import PersonFormPage from './lib/pages/PersonFormPage.svelte';
  import SearchPage from './lib/pages/SearchPage.svelte';
  import SearchBar from './lib/components/SearchBar.svelte';
  import Toasts from './lib/components/Toasts.svelte';

  let route = $derived(router.current);

  // highlight the nav link of the section we are in
  function isActive(section: string): boolean {
    return route.name.startsWith(section);
  }
</script>

<div class="app">
  <header class="topbar">
    <a class="brand" href="#/movies">Movie Library</a>

    <nav>
      <a href="#/movies" class:active={isActive('movie')}>Movies</a>
      <a href="#/people" class:active={isActive('person') || isActive('people')}>
        Cast &amp; Creators
      </a>
    </nav>

    <SearchBar />
  </header>

  <main>
    {#if route.name === 'movies'}
      <MoviesPage />
    {:else if route.name === 'movie-new'}
      <MovieFormPage />
    {:else if route.name === 'movie-edit'}
      <!-- the key block rebuilds the page when the id changes,
           so the form never shows the previous movie's values -->
      {#key route.id}
        <MovieFormPage id={route.id} />
      {/key}
    {:else if route.name === 'movie-detail'}
      {#key route.id}
        <MovieDetailPage id={route.id!} />
      {/key}
    {:else if route.name === 'people'}
      <PeoplePage />
    {:else if route.name === 'person-new'}
      <PersonFormPage />
    {:else if route.name === 'person-edit'}
      {#key route.id}
        <PersonFormPage id={route.id} />
      {/key}
    {:else if route.name === 'search'}
      <SearchPage query={route.query ?? ''} />
    {:else if route.name === 'person-detail'}
      {#key route.id}
        <PersonDetailPage id={route.id!} />
      {/key}
    {:else}
      <div class="not-found">
        <h2>Page not found</h2>
        <a class="btn btn-primary" href="#/movies">Go to movies</a>
      </div>
    {/if}
  </main>

  <Toasts />
</div>

<style>
  .app {
    min-height: 100vh;
  }

  .topbar {
    display: flex;
    align-items: center;
    gap: 1.5rem;
    flex-wrap: wrap;
    padding: 1rem 1.5rem;
    background: var(--surface);
    border-bottom: 1px solid var(--border);
  }

  .brand {
    font-size: 1.15rem;
    font-weight: 700;
    color: var(--text);
    text-decoration: none;
  }

  nav {
    display: flex;
    gap: 1rem;
  }

  nav a {
    color: var(--text-muted);
    text-decoration: none;
    padding: 0.25rem 0;
    border-bottom: 2px solid transparent;
  }

  nav a:hover {
    color: var(--text);
  }

  nav a.active {
    color: var(--text);
    border-bottom-color: var(--accent);
  }

  main {
    max-width: 1100px;
    margin: 0 auto;
    padding: 1.5rem;
  }

  .not-found {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 1rem;
    padding: 3rem 1rem;
  }

  @media (max-width: 480px) {
    .topbar {
      padding: 1rem;
      gap: 0.75rem;
    }

    main {
      padding: 1rem;
    }
  }
</style>
