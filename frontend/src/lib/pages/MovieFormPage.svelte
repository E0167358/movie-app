<script lang="ts">
  import { createMovie, fetchMovie, updateMovie } from '../api/movies';
  import { GraphQlError } from '../api/client';
  import {
    toOptionalNumber,
    toOptionalText,
    validateMovie,
    type Errors,
    type MovieFormValues,
  } from '../api/validation';
  import { router } from '../stores/router.svelte';
  import { toasts } from '../stores/toast.svelte';
  import Spinner from '../components/Spinner.svelte';
  import ErrorBox from '../components/ErrorBox.svelte';

  // no id means we are adding a new movie
  let { id }: { id?: string } = $props();

  let values = $state<MovieFormValues>({
    title: '',
    description: '',
    releaseYear: '',
    genre: '',
    durationMinutes: '',
  });

  let errors = $state<Errors>({});
  // starts false so the "add movie" form renders straight away. load() turns
  // it on when there is a movie to fetch
  let loading = $state(false);
  let saving = $state(false);
  let loadError = $state<string | null>(null);

  async function load() {
    if (!id) return;

    loading = true;
    loadError = null;

    try {
      const movie = await fetchMovie(id);
      if (!movie) {
        loadError = 'That movie does not exist anymore';
        return;
      }
      values = {
        title: movie.title,
        description: movie.description ?? '',
        releaseYear: movie.releaseYear?.toString() ?? '',
        genre: movie.genre ?? '',
        durationMinutes: movie.durationMinutes?.toString() ?? '',
      };
    } catch (error) {
      loadError = error instanceof GraphQlError ? error.message : 'Could not load the movie';
    } finally {
      loading = false;
    }
  }

  async function onSubmit(event: SubmitEvent) {
    event.preventDefault();

    errors = validateMovie(values);
    if (Object.keys(errors).length > 0) {
      return;
    }

    const input = {
      title: values.title.trim(),
      description: toOptionalText(values.description),
      releaseYear: toOptionalNumber(values.releaseYear),
      genre: toOptionalText(values.genre),
      durationMinutes: toOptionalNumber(values.durationMinutes),
    };

    saving = true;
    try {
      const movie = id ? await updateMovie(id, input) : await createMovie(input);
      toasts.success(id ? 'Movie updated' : 'Movie added');
      router.go(`/movies/${movie.id}`);
    } catch (error) {
      const message =
        error instanceof GraphQlError ? error.message : 'Could not save the movie';
      // the server may reject something the browser allowed, show it on the field
      errors = { form: message };
      toasts.error(message);
    } finally {
      saving = false;
    }
  }

  $effect(() => {
    load();
  });
</script>

<section class="form-page">
  <a class="back" href={id ? `#/movies/${id}` : '#/movies'}>&larr; Back</a>
  <h2>{id ? 'Edit movie' : 'Add movie'}</h2>

  {#if loading}
    <Spinner label="Loading movie..." />
  {:else if loadError}
    <ErrorBox message={loadError} onRetry={load} />
  {:else}
    <form onsubmit={onSubmit} novalidate>
      <div class="field">
        <label for="title">Title *</label>
        <input
          id="title"
          bind:value={values.title}
          aria-invalid={Boolean(errors.title)}
          aria-describedby={errors.title ? 'title-error' : undefined}
        />
        {#if errors.title}
          <span class="field-error" id="title-error">{errors.title}</span>
        {/if}
      </div>

      <div class="field">
        <label for="description">Description</label>
        <textarea id="description" rows="4" bind:value={values.description}></textarea>
      </div>

      <div class="row">
        <div class="field">
          <label for="releaseYear">Release year</label>
          <!-- text + inputmode instead of type="number": a number input binds a
               number (or undefined for "12ab"), which loses what the user typed
               and makes the error message impossible to show -->
          <input
            id="releaseYear"
            type="text"
            inputmode="numeric"
            bind:value={values.releaseYear}
            aria-invalid={Boolean(errors.releaseYear)}
          />
          {#if errors.releaseYear}
            <span class="field-error">{errors.releaseYear}</span>
          {/if}
        </div>

        <div class="field">
          <label for="genre">Genre</label>
          <input id="genre" bind:value={values.genre} aria-invalid={Boolean(errors.genre)} />
          {#if errors.genre}
            <span class="field-error">{errors.genre}</span>
          {/if}
        </div>

        <div class="field">
          <label for="duration">Duration (minutes)</label>
          <input
            id="duration"
            type="text"
            inputmode="numeric"
            bind:value={values.durationMinutes}
            aria-invalid={Boolean(errors.durationMinutes)}
          />
          {#if errors.durationMinutes}
            <span class="field-error">{errors.durationMinutes}</span>
          {/if}
        </div>
      </div>

      {#if errors.form}
        <p class="form-error" role="alert">{errors.form}</p>
      {/if}

      <div class="actions">
        <a class="btn" href={id ? `#/movies/${id}` : '#/movies'}>Cancel</a>
        <button class="btn btn-primary" type="submit" disabled={saving}>
          {saving ? 'Saving...' : id ? 'Save changes' : 'Add movie'}
        </button>
      </div>
    </form>

    {#if !id}
      <p class="hint">You can upload artwork after the movie is created.</p>
    {/if}
  {/if}
</section>

<style>
  .form-page {
    max-width: 640px;
  }

  .back {
    display: inline-block;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
    text-decoration: none;
  }

  h2 {
    margin: 0 0 1.5rem;
  }

  .row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    gap: 1rem;
  }

  .form-error {
    color: var(--danger);
    margin: 0 0 1rem;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 1rem;
  }

  .hint {
    margin-top: 1.5rem;
    color: var(--text-muted);
    font-size: 0.9rem;
  }
</style>
