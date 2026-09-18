<script lang="ts">
  import { deleteMovie, fetchMovie } from '../api/movies';
  import { GraphQlError } from '../api/client';
  import type { Movie } from '../api/types';
  import { router } from '../stores/router.svelte';
  import { toasts } from '../stores/toast.svelte';
  import ArtworkUpload from '../components/ArtworkUpload.svelte';
  import ConfirmDialog from '../components/ConfirmDialog.svelte';
  import CreditEditor from '../components/CreditEditor.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';
  import Spinner from '../components/Spinner.svelte';

  let { id }: { id: string } = $props();

  let movie = $state<Movie | null>(null);
  let loading = $state(true);
  let errorMessage = $state<string | null>(null);
  let confirmingDelete = $state(false);
  let deleting = $state(false);

  async function load() {
    loading = true;
    errorMessage = null;

    try {
      const result = await fetchMovie(id);
      if (!result) {
        errorMessage = 'That movie does not exist anymore';
        return;
      }
      movie = result;
    } catch (error) {
      errorMessage = error instanceof GraphQlError ? error.message : 'Could not load the movie';
    } finally {
      loading = false;
    }
  }

  async function confirmDelete() {
    deleting = true;
    try {
      await deleteMovie(id);
      toasts.success('Movie deleted');
      router.go('/movies');
    } catch (error) {
      const message =
        error instanceof GraphQlError ? error.message : 'Could not delete the movie';
      toasts.error(message);
    } finally {
      deleting = false;
      confirmingDelete = false;
    }
  }

  function onArtworkUploaded(updated: Movie) {
    // keep the credits we already loaded, the upload response does not include them
    movie = { ...updated, credits: movie?.credits };
  }

  $effect(() => {
    // reload when the id in the url changes
    id;
    load();
  });
</script>

<section>
  <a class="back" href="#/movies">&larr; All movies</a>

  {#if loading}
    <Spinner label="Loading movie..." />
  {:else if errorMessage}
    <ErrorBox message={errorMessage} onRetry={load} />
  {:else if movie}
    <header class="detail-header">
      <div>
        <h2>{movie.title}</h2>
        <p class="meta">
          {[
            movie.releaseYear,
            movie.genre,
            movie.durationMinutes ? `${movie.durationMinutes} min` : null,
          ]
            .filter(Boolean)
            .join(' · ') || 'No details yet'}
        </p>
      </div>

      <div class="header-actions">
        <a class="btn" href="#/movies/{movie.id}/edit">Edit</a>
        <button class="btn btn-danger" onclick={() => (confirmingDelete = true)}>
          Delete
        </button>
      </div>
    </header>

    <ArtworkUpload
      movieId={movie.id}
      currentArtworkUrl={movie.artworkUrl}
      onUploaded={onArtworkUploaded}
    />

    {#if movie.description}
      <p class="description">{movie.description}</p>
    {/if}

    <!-- reloading the movie after a change keeps the list and the
         "already added" checks in sync with the server -->
    <CreditEditor movieId={movie.id} credits={movie.credits ?? []} onChanged={load} />
  {/if}
</section>

{#if confirmingDelete && movie}
  <ConfirmDialog
    title="Delete movie"
    message="Delete {movie.title}? This also removes its cast and creator credits."
    busy={deleting}
    onConfirm={confirmDelete}
    onCancel={() => (confirmingDelete = false)}
  />
{/if}

<style>
  .back {
    display: inline-block;
    margin-bottom: 0.75rem;
    font-size: 0.9rem;
    text-decoration: none;
  }

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 1rem;
    flex-wrap: wrap;
    margin-bottom: 1.5rem;
  }

  h2 {
    margin: 0;
  }

  .meta {
    margin: 0.25rem 0 0;
    color: var(--text-muted);
  }

  .header-actions {
    display: flex;
    gap: 0.5rem;
  }

  .description {
    margin: 1.5rem 0 0;
    max-width: 60ch;
  }

</style>
